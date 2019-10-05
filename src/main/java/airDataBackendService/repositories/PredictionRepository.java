package airDataBackendService.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import airDataBackendService.database.Prediction;

public interface PredictionRepository extends MongoRepository<Prediction, String>, PredictionRepositoryCustom {
}