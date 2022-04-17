import groovy.sql.Sql

def output = []

def sql = Sql.newInstance('jdbc:sqlserver://DeeAutomation.mssql.somee.com:1433/DeeAutomation','manhattan1902_SQLLogin_1', 'gg2yrvf7hc')
//def sql = Sql.newInstance('jdbc:mysql:	  //localhost:3306/test', 'root', '', 														'com.mysql.jdbc.Driver')
String sqlString = "SELECT ClassName FROM regression;"
sql.eachRow(sqlString){ row ->
	output.push(row[0])
}

return output