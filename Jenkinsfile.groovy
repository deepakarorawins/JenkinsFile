import groovy.sql.Sql

def output = []

def sql = Sql.newInstance('jdbc:sqlserver://DeeAutomation.mssql.somee.com;databaseName=DeeAutomation;user=manhattan1902_SQLLogin_1;password=gg2yrvf7hc;encrypt=true;trustServerCertificate=true;')
String sqlString = "SELECT ClassName FROM regression;"
sql.eachRow(sqlString){ row ->
	output.push(row[0])
}

return output