package airDataBackendService.database;

import org.springframework.data.mongodb.repository.MongoRepository;

import airDataBackendService.database.HourlySensorData;

@Repository
public interface HourlySensorDataRepository extends MongoRepository<HourlySensorData, String>
{
	
}
