package org.gradle.backendpostgresqlapi.service;

import lombok.extern.slf4j.Slf4j;
import org.gradle.backendpostgresqlapi.entity.Timestamp;
import org.gradle.backendpostgresqlapi.repository.TimestampRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TimestampService {

    private final TimestampRepo timestampRepo;

    @Autowired
    public TimestampService(TimestampRepo timestampRepo) {
        this.timestampRepo = timestampRepo;
    }

    public void saveTimestamp(Timestamp timestamp) {
        if (timestampRepo.getMaxOneDuplicate(timestamp.getParkingPointId(), timestamp.getTimestamp()) == 0) {
            timestampRepo.save(timestamp);
        }
    }
}
