package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;
import server.Runner;
import server.models.Album;
import server.models.User;
import server.repositories.AlbumRepository;
import server.repositories.SongRepository;
import server.repositories.UserRepository;
import server.services.SongService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Runner.class)
@ActiveProfiles("test")
public class SongController {
  private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);
  private MockMvc mockMvc;
  private User user1, user2;
  private Album rtu1Album1, rtu1Album2;
  private Album rtu2Album1, rtu2Album2;
  private Principal principal1 = new UsernamePasswordAuthenticationToken("rtu1", "rtuPass1");
  private Principal principal2 = new UsernamePasswordAuthenticationToken("rtu2", "rtuPass2");
  private Principal nonExistPrincipal = new UsernamePasswordAuthenticationToken("non-existing", "p");
  private File mp3correctFile = new File(SongController.class.getResource("/mp3/MP3_700KB.mp3").getFile());
  private static File tempFolder = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());

  @Autowired
  private SongService songService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private AlbumRepository albumRepo;

  @Autowired
  private SongRepository songRepo;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeClass
  public static void setUpBeforeClass() {
    if (tempFolder != null && !tempFolder.exists()) {
      tempFolder.mkdirs();
    }
  }

  @AfterClass
  public static void  tearDownAfterClass() throws Exception {
    if (tempFolder != null && tempFolder.exists()) {
      tempFolder.delete();
    }
    tempFolder = null;
  }

  @Before
  public void setup() {
    this.mockMvc = webAppContextSetup(webApplicationContext).build();

    // mock config
    ReflectionTestUtils.setField(songService, "audioFilesDirectoryPath", tempFolder.getPath());
    ReflectionTestUtils.setField(songService, "audioFilesDirectory", tempFolder);

    user1 = new User();
    user1.setUsername("rtu1");
    user1.setPassword("rtuPass1");
    user1.setName("Rus");
    user1.setLastname("Tum");
    userRepo.save(user1);

    user2 = new User();
    user2.setUsername("rtu2");
    user2.setPassword("rtuPass2");
    user2.setName("Rus");
    user2.setLastname("Tum");
    userRepo.save(user2);

    Album album = new Album();
    album.setName("rtu1 album - public");
    album.setDescription("rtu1 album - public");
    album.setUser(user1);
    album.setInternal(false);
    rtu1Album1 = albumRepo.save(album);

    album = new Album();
    album.setName("rtu1 album - private");
    album.setDescription("rtu1 album - private");
    album.setUser(user1);
    album.setInternal(true);
    rtu1Album2 = albumRepo.save(album);

    album = new Album();
    album.setName("rtu2 album - public");
    album.setDescription("rtu2 album - public");
    album.setUser(user2);
    album.setInternal(false);
    rtu2Album1 = albumRepo.save(album);

    album = new Album();
    album.setName("rtu2 album - private");
    album.setDescription("rtu2 album - private");
    album.setUser(user2);
    album.setInternal(true);
    rtu2Album2 = albumRepo.save(album);
  }

  @Test
  public void createSongTest() throws Exception {
    MockMultipartFile mp3File = new MockMultipartFile("audio", "mp3.mp3", "audio/mp3", new FileInputStream(mp3correctFile));
    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/album/{albumId}/song", rtu1Album1.getId())
            .file(mp3File)
            .principal(principal1)
            .param("artist", "art1")
            .param("title", "song1"))
            .andExpect(status().isOk());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/album/{albumId}/song", rtu1Album2.getId())
            .file(mp3File)
            .principal(principal1)
            .param("artist", "art2")
            .param("title", "song2"))
            .andExpect(status().isOk());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/album/{albumId}/song", rtu2Album1.getId())
            .file(mp3File)
            .principal(principal2)
            .param("artist", "art2")
            .param("title", "song2"))
            .andExpect(status().isOk());

    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/album/{albumId}/song", rtu2Album2.getId())
            .file(mp3File)
            .principal(principal2)
            .param("artist", "art2")
            .param("title", "song2"))
            .andExpect(status().isOk());

    // Try to upload to another user album
    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/album/{albumId}/song", rtu1Album1.getId())
            .file(mp3File)
            .principal(principal2)
            .param("artist", "art2")
            .param("title", "song2"))
            .andExpect(status().isNotFound());

    // Try to upload empty file
    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/album/{albumId}/song", rtu1Album1.getId())
            //.file(mp3File)
            .principal(principal2)
            .param("artist", "art2")
            .param("title", "song2"))
            .andExpect(status().isBadRequest());
  }

  @After
  public void destroy() {
    userRepo.deleteAll();
    albumRepo.deleteAll();
    songRepo.deleteAll();
  }

  public String convertObjectToJsonString(Object o) throws IOException {
    return (new ObjectMapper()).writeValueAsString(o);
  }
}