package com.itsariadust.qrattendance;

import org.jdbi.v3.sqlobject.statement.*;
import org.jdbi.v3.sqlobject.customizer.*;

import java.util.Optional;
import java.time.LocalDateTime;

public interface AttendanceDao {
	@SqlQuery("""
			SELECT * FROM attendance
            WHERE StudentNo = :studentNo
            ORDER BY Timestamp DESC
            LIMIT 1
			""")
	Optional<Attendance> findLatestRecord(@Bind("studentNo") String studentNo);

	@SqlUpdate("""
			INSERT INTO attendance (StudentNo, Timestamp, Status)
            VALUES(
             	:studentNo,
                :timestamp,
                :status
            )
			""")
	void insert(
		@Bind("studentNo") String studentNo,
		@Bind("timestamp") LocalDateTime timestamp,
		@Bind("status") String status
	);
}
