@Grab('com.oracle:ojdbc6:11.2.0.4')
import groovy.sql.Sql;
import java.util.ServiceLoader;
import java.sql.Driver;

ServiceLoader<Driver> loader = ServiceLoader.load(Driver.class);
def sql = Sql.newInstance("jdbc:sqlserver://DeeAutomation.mssql.somee.com:1433;databaseName=DeeAutomation;user=manhattan1902_SQLLogin_1;password=gg2yrvf7hc;encrypt=true;trustServerCertificate=true;")
sql.execute 'select 1 from dual'
sql.close()