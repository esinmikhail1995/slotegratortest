package com.slotegrator.qa.client;

import com.slotegrator.qa.dto.CredentialsDto;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

/** Authentication endpoints. */
public interface AuthFeignClient {

    @RequestLine("POST /api/tester/login")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    Response login(CredentialsDto credentials);

    /** Same call with an explicit {@code Authorization: Basic ...} header, per the contract's BasicAuth scheme. */
    @RequestLine("POST /api/tester/login")
    @Headers({"Content-Type: application/json", "Accept: application/json", "Authorization: {authorization}"})
    Response login(@Param("authorization") String authorization, CredentialsDto credentials);
}
