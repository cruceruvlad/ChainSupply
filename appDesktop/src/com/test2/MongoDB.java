package com.test2;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Array;
import java.util.Arrays;

public class MongoDB {
    private String passwordCorrect = "admin";
    private String usernameCorrect = "admin";

    public MongoCollection connect() {
        String uri = "mongodb+srv://admin:admin@cluster0-rvxaj.mongodb.net/test";

        MongoClientURI clientURI = new MongoClientURI(uri);

        MongoClient mongoClient = new MongoClient(clientURI);

        MongoDatabase mongoDatabase = mongoClient.getDatabase("supplydb");
        MongoCollection collection = mongoDatabase.getCollection("products");

        return collection;
    }

    public boolean check(String username, String password) {
        if (username.equals(usernameCorrect) && password.equals(passwordCorrect)) {
            return true;
        }
        return false;
    }

}
