package js.transaction.jdbc;

import js.lang.Config;
import js.lang.ConfigException;
import js.log.Log;
import js.log.LogFactory;
import js.transaction.Transaction;
import js.transaction.TransactionException;
import js.transaction.TransactionManager;
import js.transaction.WorkingUnit;

/**
 * Implementation for {@link TransactionManager} interface.
 * 
 * @author Iulian Rotaru
 */
final class TransactionManagerImpl implements TransactionManager {
	private static final Log log = LogFactory.getLog(TransactionManagerImpl.class);

	private JdbcAdapter adapter;

	public TransactionManagerImpl() {
		this.adapter = new JdbcAdapter();
	}

	@Override
	public void config(Config config) throws ConfigException {
		log.trace("config(Config)");
		adapter.config(config);
	}

	@Override
	public Transaction createTransaction() {
		return adapter.createTransaction(false);
	}

	@Override
	public Transaction createReadOnlyTransaction() {
		return adapter.createTransaction(true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S, T> T exec(WorkingUnit<S, T> workingUnit, Object... args) {
		Transaction t = createTransaction();
		T o = null;
		try {
			o = (T) workingUnit.exec((S) t.getSession(), args);
			t.commit();
		} catch (Exception e) {
			t.rollback();
			throw new TransactionException(e);
		} finally {
			t.close();
		}
		return o;
	}

	@Override
	public void destroy() {
		adapter.destroy();
	}
}
