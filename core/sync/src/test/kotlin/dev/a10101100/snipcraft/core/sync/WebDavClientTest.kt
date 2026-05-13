package dev.a10101100.snipcraft.core.sync

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebDavClientTest {

    private val server = MockWebServer()
    private val client = WebDavClient(OkHttpClient())
    private val creds = WebDavClient.Credentials("user", "pass")

    @BeforeEach fun start() { server.start() }
    @AfterEach  fun stop()  { server.shutdown() }

    private fun url(path: String = "/snipcraft/snippets.json") =
        server.url(path).toString()

    @Test
    fun `propFind returns Found when server responds 207`() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody("<multistatus/>"))
        val result = client.propFind(url(), creds)
        assertEquals(WebDavClient.PropFindResult.Found, result)
    }

    @Test
    fun `propFind returns NotFound when server responds 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = client.propFind(url(), creds)
        assertEquals(WebDavClient.PropFindResult.NotFound, result)
    }

    @Test
    fun `propFind sends PROPFIND method with Basic auth`() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody("<multistatus/>"))
        client.propFind(url(), creds)
        val req: RecordedRequest = server.takeRequest()
        assertEquals("PROPFIND", req.method)
        assertTrue(req.getHeader("Authorization")?.startsWith("Basic ") == true)
    }

    @Test
    fun `get returns body when server responds 200`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"version":1}"""))
        val body = client.get(url(), creds)
        assertEquals("""{"version":1}""", body)
    }

    @Test
    fun `get returns null when server responds 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val body = client.get(url(), creds)
        assertNull(body)
    }

    @Test
    fun `put returns true when server responds 201`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        val ok = client.put(url(), creds, """{"version":1}""")
        assertTrue(ok)
        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("""{"version":1}""", req.body.readUtf8())
    }

    @Test
    fun `put returns false when server responds 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertFalse(client.put(url(), creds, "body"))
    }

    @Test
    fun `mkCol returns true when server responds 201`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        assertTrue(client.mkCol(url("/snipcraft"), creds))
        assertEquals("MKCOL", server.takeRequest().method)
    }

    @Test
    fun `mkCol returns true when server responds 405 — directory already exists`() = runTest {
        server.enqueue(MockResponse().setResponseCode(405))
        assertTrue(client.mkCol(url("/snipcraft"), creds))
    }

    @Test
    fun `rejects plain HTTP when allowHttp is false`() = runTest {
        val httpUrl = "http://example.com/file.json"
        val result = client.propFind(httpUrl, creds, allowHttp = false)
        assertTrue(result is WebDavClient.PropFindResult.Error)
        val msg = (result as WebDavClient.PropFindResult.Error).message
        assertTrue(msg.contains("plain HTTP", ignoreCase = true))
    }
}
