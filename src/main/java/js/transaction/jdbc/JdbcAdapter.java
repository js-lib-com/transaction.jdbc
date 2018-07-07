package js.transaction.jdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import js.lang.Config;
import js.lang.ConfigException;
import js.lang.Configurable;
import js.log.Log;
import js.log.LogFactory;
import js.transaction.Transaction;
import js.transaction.TransactionException;
import js.util.Classes;

import com.mchange.v2.c3p0.DataSources;

/**
 * JDBC database adapter.
 */
final class JdbcAdapter implements Configurable {
	private static final Log log = LogFactory.getLog(JdbcAdapter.class);

	/** Thread local storage. Use this thread specific storage to cache the database handler attached to current transaction. */
	private ThreadLocal<TransactionImpl> tls = new ThreadLocal<TransactionImpl>();

	/**
	 * JDBC data source. It is used as a JDBC connection factory, connection being the native interface of
	 * {@link TransactionImpl}.
	 */
	private DataSource dataSource;

	/**
	 * Database initialization. Read properties from configuration context and create a pooled data source. Also configure
	 * connections pool from the same context.
	 * 
	 * @throws ConfigException if database driver class not found or data source creation fails.
	 */
	@Override
	public void config(Config config) throws ConfigException {
		String driver = config.getProperty("driver");
		String url = config.getProperty("url");
		String user = config.getProperty("user");
		String password = config.getProperty("password");
		Class<?> driverClass = Classes.forOptionalName(driver);
		if (driverClass == null) {
			throw new ConfigException("Missing database driver class |%s|.", driver);
		}

		int minPoolSize = config.getProperty("minPoolSize", int.class, 0);
		int maxPoolSize = config.getProperty("maxPoolSize", int.class, 0);
		int maxStatements = config.getProperty("maxStatements", int.class, 0);

		if (maxPoolSize > 0 && maxPoolSize < 4) {
			maxPoolSize = 4;
		}
		if (minPoolSize > 0 && minPoolSize > maxPoolSize) {
			minPoolSize = maxPoolSize;
		}
		if (maxStatements > 0 && maxStatements < maxPoolSize) {
			maxStatements = maxPoolSize;
		}

		if (minPoolSize != 0) {
			config.setProperty("minPoolSize", minPoolSize);
		}
		if (maxPoolSize != 0) {
			config.setProperty("maxPoolSize", maxPoolSize);
		}
		if (maxStatements != 0) {
			config.setProperty("maxStatements", maxStatements);
		}

		try {
			DataSource ds = DataSources.unpooledDataSource(url, user, password);
			this.dataSource = DataSources.pooledDataSource(ds, config.getUnusedProperties());
		} catch (SQLException e) {
			throw new ConfigException("JDBC adapter initialization fail due to bad configuration.");
		}
	}

	public void destroyHandler() {
		tls.set(null);
	}

	@SuppressWarnings("unchecked")
	public <T> T getNativeInterface() {
		Transaction handler = tls.get();
		if (handler == null) {
			throw new TransactionException("Missing transaction handler. Probably attempt to use native interface outside a transaction boundaries.");
		}
		return (T) handler.getSession();
	}

	/**
	 * Create a database handler. Create a new {@link Transaction} and store it on current thread so that it can be retrieved by
	 * application code. This method is invoked by {@link TransactionManagerImpl} when start a new transaction. Note that
	 * created handler is valid only on current transaction boundaries.
	 * 
	 * @param readOnly if this flag is true create a read-only handler, that is, supporting only database select operations.
	 * @return newly created database handler.
	 */
	public Transaction createTransaction(boolean readOnly) {
		TransactionImpl transaction = tls.get();
		if (transaction != null) {
			transaction.incrementTransactionNestingLevel();
		} else {
			transaction = new TransactionImpl(this, dataSource, readOnly);
			tls.set(transaction);
		}
		return transaction;
	}

	public void destroy() {
		log.debug("Dispose JDBC database adapter.");
		try {
			DataSources.destroy(dataSource);
		} catch (SQLException e) {
			log.error(e);
		}
	}
}
