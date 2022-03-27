const db = new Mongo().getDB('users');
db.getCollection('topicData').createIndex({
    "requestId": 1
});
db.getCollection('topicData').createIndex({
    "createdAt": 1
},
{
    expireAfterSeconds: 1800
});