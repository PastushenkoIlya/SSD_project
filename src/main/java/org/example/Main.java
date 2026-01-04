package org.example;

import org.example.client.Client;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Client client = null;
        try {
            client = new Client();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
        client.run();
    }
}