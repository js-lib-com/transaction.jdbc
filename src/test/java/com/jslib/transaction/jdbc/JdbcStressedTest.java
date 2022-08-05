package com.jslib.transaction.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.Ignore;

import js.transaction.Transaction;
import js.transaction.TransactionManager;
import js.util.Classes;
import junit.framework.TestCase;

@Ignore
public class JdbcStressedTest extends TestCase
{
  @Override
  protected void setUp() throws Exception
  {
  }

  public void testStressedJdbcTransaction() throws Exception
  {
    TransactionManager transactionManager = Classes.newInstance("js.jdbc.TransactionManagerImpl");

    for(int i = 0; i < 1000000; i++) {
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
