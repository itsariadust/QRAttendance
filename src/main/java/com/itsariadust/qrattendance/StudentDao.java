package com.itsariadust.qrattendance;

import org.jdbi.v3.sqlobject.statement.*;
import org.jdbi.v3.sqlobject.customizer.*;

import java.util.Optional;

public interface StudentDao {
	@SqlQuery("""
			SELECT * FROM students WHERE StudentNo = :studentNo
			""")
	Optional<Students> findStudent(@Bind("studentNo") String studentNo);
}
