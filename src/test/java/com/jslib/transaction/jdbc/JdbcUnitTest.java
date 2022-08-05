package com.jslib.transaction.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import js.lang.Config;
import js.lang.ConfigBuilder;
import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.transaction.WorkingUnit;
import junit.framework.TestCase;

public class JdbcUnitTest extends TestCase
{
  private Config config;

  @Override
  protected void setUp() throws Exception
  {
    Properties properties = new Properties();
    properties.setProperty("url", "jdbc:mysql://localhost:3306/test?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
    properties.setProperty("user", "test");
    properties.setProperty("password", "test");
    properties.setProperty("initialPoolSize", "5");
    properties.setProperty("minPoolSize", "5");
    properties.setProperty("maxPoolSize", "140");
    properties.setProperty("maxStatements", "50");
    properties.setProperty("acquireIncrement", "3");
    properties.setProperty("maxIdleTime", "1800");
    properties.setProperty("idleConnectionTestPeriod", "0");
    properties.setProperty("testConnectionsOnCheckout", "false");

    ConfigBuilder builder = new ConfigBuilder(properties);
    config = builder.build();
  }

  public void testExecuteProgrammaticTransaction() throws Exception
  {
    TransactionManager transactionManager = new TransactionManagerImpl();
    transactionManager.config(config);

    final Person person = new Person();
    Book book = transactionManager.exec(new WorkingUnit<Connection, Book>()
    {
      public Book exec(Connection connection, Object... args) throws SQLException
      {
        Person p = (Person)args[0];
        assertEquals(person.id, p.id);
        assertEquals(person.name, p.name);

        PreparedStatement ps = connection.prepareStatement("INSERT INTO person (name,surname,landline,mobile,emailAddr) VALUES(?,?,?,?,?)");
        ps.setString(1, person.name);
        ps.setString(2, person.surname);
        ps.setString(3, person.landline);
        ps.setString(4, person.mobile);
        ps.setString(5, person.emailAddr);
        ps.executeUpdate();

        return new Book();
      }
    }, person);
    assertNotNull(book);
  }

  public void testExecuteJdbcTransaction() throws Exception
  {
    TransactionManager transactionManager = new TransactionManagerImpl();
    transactionManager.config(config);
    Transaction t = transactionManager.createTransaction();

    Connection connection = (Connection)t.getSession();
    Person person = new Person();
    PreparedStatement ps = connection.prepareStatement("INSERT INTO person (name,surname,landline,mobile,emailAddr) VALUES(?,?,?,?,?)");
    ps.setString(1, person.name);
    ps.setString(2, person.surname);
    ps.setString(3, person.landline);
    ps.setString(4, person.mobile);
    ps.setString(5, person.emailAddr);
    ps.executeUpdate();

    t.commit();
    t.close();
  }

  public void executeJdbcConcurentTransaction() throws Exception
  {
    class TransactionThread extends Thread
    {
      @SuppressWarnings("unused")
      private int id;

      public TransactionThread(int id)
      {
        this.id = id;
      }

      public void run()
      {
        TransactionManager transactionManager = new TransactionManagerImpl();

        for(int i = 0; i < 100; i++) {
          Transaction t = transactionManager.createTransaction();
          try {
            Connection connection = (Connection)t.getSession();
            Person person = new Person();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO person (name,surname,landline,mobile,emailAddr) VALUES(?,?,?,?,?)");
            ps.setString(1, person.name);
            ps.setString(2, person.surname);
            ps.setString(3, person.landline);
            ps.setString(4, person.mobile);
            ps.setString(5, person.emailAddr.toString());
            ps.executeUpdate();
            t.commit();
          }
          catch(Exception e) {
            t.rollback();
          }
          finally {
            t.close();
          }
        }
      }
    }

    final int COUNT = 10;
    Thread[] threads = new Thread[COUNT];
    for(int i = 0; i < COUNT; i++) {
      threads[i] = new TransactionThread(i);
      threads[i].start();
    }
    for(int i = 0; i < COUNT; i++) {
      threads[i].join();
    }
  }
}
