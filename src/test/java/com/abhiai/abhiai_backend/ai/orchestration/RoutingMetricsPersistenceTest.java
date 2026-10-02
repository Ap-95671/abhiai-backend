package com.abhiai.abhiai_backend.ai.orchestration;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import com.abhiai.abhiai_backend.ai.AiCompletion;
import com.abhiai.abhiai_backend.ai.pipeline.Intent;
import static org.assertj.core.api.Assertions.*;

/** Runs only against the explicitly created disposable local cluster, in a fresh isolated schema. */
@EnabledIfSystemProperty(named="ai.test.postgres",matches="true")
class RoutingMetricsPersistenceTest {
    private JdbcTemplate jdbc;
    private DriverManagerDataSource source;
    private String schema;
    private RoutingMetricsStore store;
    private RoutingMetrics metrics;
    private final RoutingMetrics.Key key=new RoutingMetrics.Key("openai","test-model",Intent.CODING,TaskType.CODE,ExecutionStrategy.MULTI_MODEL_REVIEW);
    @BeforeEach void setup() throws Exception {
        schema="ai_test_"+UUID.randomUUID().toString().replace("-","");
        var admin=new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55483/postgres",System.getProperty("user.name"),""));
        admin.execute("CREATE SCHEMA "+schema);
        source=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:55483/postgres?currentSchema="+schema,System.getProperty("user.name"),"");
        jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE messages(id UUID PRIMARY KEY, content TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE message_citations(message_id UUID REFERENCES messages(id), position INTEGER, title VARCHAR(500), url VARCHAR(2048), domain VARCHAR(255))");
        jdbc.update("INSERT INTO messages(id,content) VALUES (?,?)",UUID.randomUUID(),"Existing conversation content");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V38__ai_execution_feedback_and_metrics.sql")));
        store=new RoutingMetricsStore(jdbc);metrics=new RoutingMetrics(store,Runnable::run);
    }
    @AfterEach void cleanup(){if(jdbc!=null)jdbc.execute("DROP SCHEMA "+schema+" CASCADE");}
    @Test void additiveMigrationPreservesMessagesAndMetricsSurviveRestart(){
        assertThat(jdbc.queryForObject("SELECT content FROM messages",String.class)).isEqualTo("Existing conversation content");
        metrics.record(key,true,120,new AiCompletion("answer","openai","test-model",null,10,4,120,false),false);
        metrics.record(key,false,50,null,true);
        var restored=new RoutingMetrics(store,Runnable::run).snapshot("openai","test-model",TaskType.CODE);
        assertThat(restored.attempts()).isEqualTo(2);assertThat(restored.successes()).isEqualTo(1);
        assertThat(restored.inputTokens()).isEqualTo(10);assertThat(restored.outputTokens()).isEqualTo(4);
        assertThat(restored.fallbacks()).isEqualTo(1);
    }
    @Test void concurrentNativeUpsertsDoNotLoseCounts()throws Exception{
        var executor=Executors.newFixedThreadPool(4);
        try{
            var tasks=new ArrayList<Callable<Void>>();
            for(int i=0;i<40;i++)tasks.add(()->{metrics.record(key,true,10,new AiCompletion("ok"),false);return null;});
            for(var task:executor.invokeAll(tasks))task.get();
        }finally{executor.shutdownNow();}
        assertThat(new RoutingMetrics(store,Runnable::run).snapshot("openai","test-model",TaskType.CODE).attempts()).isEqualTo(40);
    }
    @Test void feedbackRollbackDoesNotAlterDurableOrLiveRoutingSignal(){
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.executeWithoutResult(status->metrics.feedback(key,null,true));
        assertThat(metrics.snapshot("openai","test-model",TaskType.CODE).positive()).isEqualTo(1);
        assertThatThrownBy(()->transaction.executeWithoutResult(status->{metrics.feedback(key,true,false);throw new IllegalStateException("rollback");})).isInstanceOf(IllegalStateException.class);
        var restored=new RoutingMetrics(store,Runnable::run).snapshot("openai","test-model",TaskType.CODE);
        assertThat(restored.positive()).isEqualTo(1);assertThat(restored.negative()).isZero();
        assertThat(metrics.snapshot("openai","test-model",TaskType.CODE).positive()).isEqualTo(1);
        transaction.executeWithoutResult(status->metrics.feedback(key,true,false));
        assertThat(new RoutingMetrics(store,Runnable::run).snapshot("openai","test-model",TaskType.CODE).negative()).isEqualTo(1);
    }
}
