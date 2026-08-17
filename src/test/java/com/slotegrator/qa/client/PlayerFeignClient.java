package com.slotegrator.qa.client;

import com.slotegrator.qa.dto.PlayerRequestDto;
import com.slotegrator.qa.dto.PlayerRequestOneDto;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

/** Player endpoints; all of them require {@code Authorization: Bearer <token>}. */
@Headers({"Content-Type: application/json", "Accept: application/json"})
public interface PlayerFeignClient {

    @RequestLine("POST /api/automationTask/create")
    Response create(PlayerRequestDto player);

    @RequestLine("POST /api/automationTask/getOne")
    Response getOne(PlayerRequestOneDto request);

    @RequestLine("GET /api/automationTask/getAll")
    Response getAll();

    /** {@code id} is a hex string in practice, not the {@code integer} the contract declares. */
    @RequestLine("DELETE /api/automationTask/deleteOne/{id}")
    Response deleteOne(@Param("id") String id);
}
