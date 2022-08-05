package com.jslib.transaction.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import js.transaction.Transaction;
import js.transaction.TransactionException;

/**
 * JDBC implementation of {@link Transaction} interface.
 * 
 * @author Iulian Rotaru
 */
final class TransactionImpl implements Transaction
{
  /** JDBC connection. This is the native interface of JDBC handler. */
  private Connection connection;

  private int nestingLevel;
  protected JdbcAdapter database;
  protected boolean readOnly = false;
  private boolean unused = true;
  private boolean closed = false;

  public TransactionImpl(JdbcAdapter database, DataSource dataSource, boolean readOnly)
  {
    this.database = database;
    try {
      this.connection = dataSource.getConnection();
      // read-only transaction does not conclude with commit and rely on driver to do it by setting auto-commit true
      // on the other hand, writable transaction ends with explicit commit and should disable auto-commit
      this.connection.setAutoCommit(readOnly);
    }
    catch(SQLException e) {
      throw new TransactionException(e);
    }
  }

  @Override
  public void commit()
  {
    if(nestingLevel > 0) {
      return;
    }
    if(readOnly) {
      throw new IllegalStateException("Read-only handler doesn't allow commit.");
    }
    try {
      connection.commit();
    }
    catch(Exception e) {
      throw new TransactionException(e);
    }
    finally {
      close();
    }
  }

  @Override
  public void rollback()
  {
    if(nestingLevel > 0) {
      return;
    }
    if(readOnly) {
      throw new IllegalStateException("Read-only handler doesn't allow roll-back.");
    }
    try {
      connection.rollback();
    }
    catch(Exception e) {
      throw new TransactionException(e);
    }
    finally {
      close();
    }
  }

  @Override
  public boolean close()
  {
    if(closed) {
      return true;
    }
    if(nestingLevel-- > 0) {
      return false;
    }
    closed = true;
    try {
      connection.close();
    }
    catch(Exception e) {
      throw new TransactionException(e);
    }
    finally {
      database.destroyHandler();
    }
    return true;
  }

  @Override
  public boolean unused()
  {
    return unused;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getSession()
  {
    if(closed) {
      throw new IllegalStateException("Native interface was closed.");
    }
    unused = false;
    return (T)connection;
  }

  public void incrementTransactionNestingLevel()
  {
    nestingLevel++;
  }
}
