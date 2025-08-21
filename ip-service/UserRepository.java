public interface UserRepository extends MongoRepository<User, String> {
    
    @Query("{'_id': ?0}")
    @Update("{'$pull': {'connectionConfig.ipWhitelist': {'_id': ?1}}}")
    void pullIPWhitelist(String userId, String ipId);
    
    @Query("{'connectionConfig.ipWhitelist.status': 'pending_delete'}")
    List<User> findUsersWithPendingDeleteIPs();
    @Query("{'connectionConfig.ipWhitelist.status': { $in: ['pending_activate', 'pending_delete'] }}")
    List<User> findUsersWithPendingActions();
}