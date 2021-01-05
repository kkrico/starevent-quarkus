package com.rmarcello.starevent;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

import org.hamcrest.core.Is;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static javax.ws.rs.core.Response.Status.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.rmarcello.starevent.model.Event;
import com.rmarcello.starevent.repository.EventRepository;
import com.rmarcello.starevent.util.EventUtil;

@QuarkusTest
@QuarkusTestResource(TestLifecycleManager.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventResourceTest {

    private static Logger LOGGER = Logger.getLogger(EventResourceTest.class);

    private HashMap<Long, Event> eventsMap = new HashMap<>();
    private static int numberOfEvents;
    private static String eventId;

    @Test
    @Order(1)
    public void testBaseEndpoint() {    

        List<Event> eventList = new ArrayList<>();
        eventList = given()
          .when().get("/api/events")
          .then()
             .statusCode(OK.getStatusCode())
             .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
             .extract().body().as(getEventTypeRef());

        numberOfEvents=eventList.size();
        LOGGER.debug("numberOfEvents="+numberOfEvents);
    }

    private TypeRef<List<Event>> getEventTypeRef() {
        return new TypeRef<List<Event>>() {
        };
    }

    @Test
    @Order(2)
    void shouldAddAnItem() {
        
        Event eventToAdd = EventUtil.createTestEvent();
        LOGGER.debug("adding an event");
        // Persists a new event
        String location = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(eventToAdd)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .when().post("/api/events")
                .then()
                    .statusCode(CREATED.getStatusCode()).extract().header("Location");
        LOGGER.debug("created: " + location);

        // Extracts the Location and stores the event id
        assertTrue(location.contains("/api/events"));
        String[] segments = location.split("/");
        eventId = segments[segments.length - 1];
        assertNotNull(eventId);
        LOGGER.debug("eventId: " + eventId);

        // Checks the event has been created
        given()
            .pathParam("id", eventId)
            .when().get("/api/events/{id}")
                .then().statusCode(OK.getStatusCode())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body("title", Is.is(eventToAdd.getTitle()))
                .body("description", Is.is(eventToAdd.getDescription()))
                .body("address", Is.is(eventToAdd.getAddress()))
                .body("price", Is.is(eventToAdd.getPrice()))
                .body("location", Is.is(eventToAdd.getLocation()))
                ;
                
        // Checks there is an event book in the database
        List<Event> events = given().when().get("/api/events").then().statusCode(OK.getStatusCode())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).extract().body().as(getEventTypeRef());
        assertEquals(numberOfEvents + 1, events.size());
    }

    @Test
    @Order(3)
    void updateTest() {

        Event eventToUpdate = EventUtil.createTestEvent();
        eventToUpdate.setId(Long.parseLong(this.eventId));

        LOGGER.debug("updating an event");
        // updating an event
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(eventToUpdate)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .when().put("/api/events")
            .then()
                .statusCode(OK.getStatusCode())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body("title", Is.is(eventToUpdate.getTitle()))
                .body("description", Is.is(eventToUpdate.getDescription()))
                .body("address", Is.is(eventToUpdate.getAddress()))
                .body("price", Is.is(eventToUpdate.getPrice()))
                .body("location", Is.is(eventToUpdate.getLocation()));
        
    }

    @Test
    @Order(4)
    void deleteTest() {
        
        LOGGER.debug("deleting an event");
        // deleting an event
        given()
            .pathParam("id", eventId)
            .when().delete("/api/events/{id}")
                .then().statusCode( NO_CONTENT.getStatusCode());
        
        // Checks deletion
        given()
        .pathParam("id", eventId)
        .when().get("/api/events/{id}")
            .then().statusCode(NOT_FOUND.getStatusCode());
    }
}