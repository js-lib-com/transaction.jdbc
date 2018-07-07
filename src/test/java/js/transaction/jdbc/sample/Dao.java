package js.transaction.jdbc.sample;

import js.transaction.jdbc.Person;

// @Transactional
public interface Dao {
	void create(Person person);

	void delete(Person person);

	// @Immutable
	Person read(int id);

	void update(Person person);
}