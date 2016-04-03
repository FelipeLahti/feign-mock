/**
 * Copyright (C) 2016 Marvin Herman Froeder (marvin@marvinformatics.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package feign.mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static feign.Util.toByteArray;

import feign.Feign;
import feign.FeignException;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;

public class MockClientTest {

    interface GitHub {
        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);

        @RequestLine("GET /repos/{owner}/{repo}/contributors?client_id={client_id}")
        List<Contributor> contributors(@Param("client_id") String clientId, @Param("owner") String owner,
                @Param("repo") String repo);

        @RequestLine("PATCH /repos/{owner}/{repo}/contributors")
        List<Contributor> patchContributors(@Param("owner") String owner, @Param("repo") String repo);
    }

    static class Contributor {
        String login;
        int contributions;
    }

    private GitHub github;

    @Before
    public void setup() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/fixtures/contributors.json");) {
            byte[] data = toByteArray(input);
            github = Feign.builder()
                    .decoder(new GsonDecoder())
                    .client(new MockClient()
                            .ok(HttpMethod.GET, "mock:///repos/netflix/feign/contributors", data)
                            .ok(HttpMethod.GET, "mock:///repos/netflix/feign/contributors?client_id=55", "")
                            .ok(HttpMethod.GET, "mock:///repos/netflix/feign/contributors?client_id=7 7", new ByteArrayInputStream(data))
                            .noContent(HttpMethod.PATCH, "mock:///repos/velo/feign-mock/contributors"))
                    .target(GitHub.class, "mock://");
        }
    }

    @Test
    public void hitMock() {
        List<Contributor> contributors = github.contributors("netflix", "feign");
        assertThat(contributors, hasSize(30));
    }

    @Test
    public void missMock() {
        try {
            github.contributors("velo", "feign-mock");
            fail();
        } catch (FeignException e) {
            assertThat(e.getMessage(), Matchers.containsString("404"));
        }
    }

    @Test
    public void missHttpMethod() {
        try {
            github.patchContributors("netflix", "feign");
            fail();
        } catch (FeignException e) {
            assertThat(e.getMessage(), Matchers.containsString("404"));
        }
    }

    @Test
    public void paramsEncoding() {
        List<Contributor> contributors = github.contributors("7 7", "netflix", "feign");
        assertThat(contributors, hasSize(30));
    }

}
