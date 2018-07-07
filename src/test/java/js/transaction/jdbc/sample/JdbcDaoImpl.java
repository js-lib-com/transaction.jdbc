package js.transaction.jdbc.sample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import js.transaction.TransactionContext;
import js.transaction.jdbc.Person;

class JdbcDaoImpl implements Dao
{
  private TransactionContext database;

  public JdbcDaoImpl()
  {
  }

  @Override
  public void create(Person person)
  {
    try {
      Connection c = database.getSession();
      PreparedStatement ps = c.prepareStatement("INSERT INTO person (name,surname,landline,mobile,emailAddr) VALUES(?,?,?,?,?)");
      ps.setString(1, person.name);
      ps.setString(2, person.surname);
      ps.setString(3, person.landline);
      ps.setString(4, person.mobile);
      ps.setString(5, person.emailAddr.toString());
      ps.executeUpdate();
    }
    catch(SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void delete(Person person)
  {
  }

  @Override
  public Person read(int id)
  {
    return null;
  }

  @Override
  public void update(Person person)
  {
  }
}
