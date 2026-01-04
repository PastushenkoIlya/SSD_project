package org.example;

import org.example.client.Client;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.run();
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}