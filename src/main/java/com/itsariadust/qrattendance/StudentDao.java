package com.itsariadust.qrattendance;

import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.statement.*;
import org.jdbi.v3.sqlobject.customizer.*;

import java.util.Optional;

public interface StudentDao {
	@SqlQuery("""
			SELECT * FROM students WHERE StudentNo = :studentNo
			""")
	Optional<Students> findStudent(@Bind("studentNo") String studentNo);

	@SqlQuery("""
			SELECT Picture FROM students WHERE StudentNo = :studentNo
			""")
	@SingleValue
	byte[] findPicture(@Bind("studentNo") String studentNo);
}
