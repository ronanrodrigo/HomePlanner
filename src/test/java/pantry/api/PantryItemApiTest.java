package pantry.api;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterableOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import hrp.HomePlannerApp;
import hrp.pantry.enums.PackagingUnit;
import hrp.pantry.persistence.entities.PantryItem;
import hrp.pantry.persistence.entities.Product;
import hrp.pantry.persistence.repositories.PantryItemRepository;
import hrp.pantry.persistence.repositories.ProductRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = HomePlannerApp.class, loader = SpringBootContextLoader.class)
public class PantryItemApiTest {

  @LocalServerPort
  String port;

  @Autowired
  PantryItemRepository itemRepository;

  @Autowired
  ProductRepository productRepository;

  @Before
  public void setUp() {
    RestAssured.baseURI = "http://localhost:" + port;
    itemRepository.deleteAll();
  }

  @Test
  public void blackBoxItemCreationTest() throws JSONException {
    JSONObject item = new JSONObject()
        .put("eanCode", "1234567890123")
        .put("name", "Coca Cola 2l")
        .put("quantity", 10)
        .put("unit", "BOTTLE");

    RestAssured
        .given()
          .contentType(ContentType.JSON)
          .body(item.toString())
        .when()
          .post("/pantry/item")
        .then()
          .contentType(ContentType.JSON)
          .statusCode(200)
          .body("name", equalTo(item.get("name")))
          .body("quantity", equalTo(item.get("quantity")))
          .body("unit", equalTo(item.get("unit")))
          .body("uuid", notNullValue())
          .body("createdAt", notNullValue())
          .body("updatedAt", notNullValue());

    List<Product> retrievedProducts = (List<Product>) productRepository.findProductsByEanCode((String)item.get("eanCode"));
    List<PantryItem> retrievedItems = (List<PantryItem>) itemRepository.findAll();
    assertThat(retrievedItems.size(), is(equalTo(1)));
    assertThat(retrievedProducts.size(), is(equalTo(1)));
    assertThat(retrievedProducts.get(0).getName(), is(equalTo((String) item.get("name"))));
    assertThat(retrievedProducts.get(0).getUnit().toString(), is(equalTo(item.get("unit"))));
    assertThat(retrievedProducts.get(0).getEanCode(), is(equalTo((String) item.get("eanCode"))));
  }

  @Test
  public void retrieveAllCreatedItemsTest(){
    PantryItem item1 = new PantryItem("Erdinger Kristall 500ml",1, PackagingUnit.BOTTLE);
    PantryItem item2 = new PantryItem("Erdinger Kristall 500ml",2, PackagingUnit.BOTTLE);
    List<PantryItem> items = Arrays.asList(item1,item2);
    itemRepository.save(items);

    RestAssured
        .given()
          .contentType(ContentType.JSON)
        .when()
          .get("/pantry/item")
        .then()
          .statusCode(200)
          .body("get(0).name", is(equalTo(item1.getName())))
          .body("get(0).quantity", equalTo(item1.getQuantity()))
          .body("get(0).unit", equalTo(item1.getUnit().toString()))
          .body("get(0).uuid", notNullValue())
          .body("get(0).createdAt", notNullValue())
          .body("get(0).updatedAt", notNullValue())
          .body("get(1).name", is(equalTo(item2.getName())))
          .body("get(1).quantity", equalTo(item2.getQuantity()))
          .body("get(1).unit", equalTo(item2.getUnit().toString()))
          .body("get(1).uuid", notNullValue())
          .body("get(1).createdAt", notNullValue())
          .body("get(1).updatedAt", notNullValue());
  }

  @Test
  public void deleteItemByUuidTest(){
    PantryItem item = new PantryItem(
        "Coca Cola 500ml"+ System.currentTimeMillis(),
        2,
        PackagingUnit.BOTTLE
    );
    UUID uuid = itemRepository.save(item).getUuid();

    RestAssured
        .given()
          .contentType(ContentType.JSON)
        .when()
          .delete("/pantry/item/" + uuid)
        .then()
          .statusCode(200)
          .body(is(""));

    assertThat(itemRepository.findAll(), emptyIterableOf(PantryItem.class));
  }
}
