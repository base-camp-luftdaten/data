package airDataBackendService.repositories;

import java.io.File;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface HeatmapRepository extends MongoRepository<File, String>, HeatmapRepositoryCustom {
}