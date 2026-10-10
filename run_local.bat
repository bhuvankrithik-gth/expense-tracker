@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "SPRING_DATASOURCE_URL=jdbc:h2:mem:expense_tracker;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
set "SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver"
set "SPRING_DATASOURCE_USERNAME=sa"
set "SPRING_DATASOURCE_PASSWORD="
set "PORT=8085"

"%JAVA_HOME%\bin\java.exe" -Dserver.port=8085 -jar target/expense-tracker-1.0.0.jar
