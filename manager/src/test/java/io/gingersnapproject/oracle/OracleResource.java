package io.gingersnapproject.oracle;

import static io.gingersnapproject.database.DatabaseResourcesLifecyleManager.Database;
import static io.gingersnapproject.database.DatabaseResourcesLifecyleManager.loadProperties;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

import java.util.Map;

import org.testcontainers.containers.OracleContainer;

/**
 * TODO!
 */
public class OracleResource implements Database {

   private static final String IMAGE = "gvenzl/oracle-xe:21-slim";
   // use database XEPDB1 already available in *-faststart image
   private static final String REACTIVE_URL_FORMAT = "oracle:thin:@%s:%s/XEPDB1";

   private OracleContainer db;

   @Override
   public void initProperties(Map<String, String> props) {
      props.put("quarkus.datasource.username", db.getUsername());
      props.put("quarkus.datasource.password", db.getPassword());
      props.put("quarkus.datasource.reactive.url", REACTIVE_URL_FORMAT.formatted(db.getHost(), db.getOraclePort()));
      loadProperties("oracle/oracle-test.properties", props);
   }

   @Override
   public void start() {
      db = new OracleContainer(IMAGE)
            .withUsername("gingersnap")
            .withPassword("password")
            .withCopyFileToContainer(forClasspathResource("oracle/oracle-setup.sql"), "/docker-entrypoint-initdb.d/001_setup.sql")
            .withCopyFileToContainer(forClasspathResource("oracle/populate.sh"), "/docker-entrypoint-initdb.d/002_populate.sh")
            .withCopyFileToContainer(forClasspathResource("populate.sql"), "/sql/populate.sql");
      db.start();
   }

   @Override
   public void stop() {
      if (db != null) {
         db.stop();
         db = null;
      }
   }
}
