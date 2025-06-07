package com.teedjay;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.io.IOException;

@Path("/image")
public class ImageResource {


    // JSON you need to provide for Redirect Plugin to capture data on your local network
    // { "filename" : "get-image", "url" : "http://192.168.0.36:8080/image", "refresh_rate" : 60 }

    @GET
    @Produces("image/png")
    public byte[] image() throws IOException {
        return this.getClass().getResource("/output-bm3.bmp").openStream().readAllBytes();
    }

}
