package com.slotegrator.qa.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.slotegrator.qa.client.PlayerFeignClient;
import com.slotegrator.qa.dto.PlayerRequestDto;
import com.slotegrator.qa.dto.PlayerRequestOneDto;
import com.slotegrator.qa.dto.PlayerResponseDto;
import com.slotegrator.qa.http.ApiResponse;
import com.slotegrator.qa.http.FeignClients;
import com.slotegrator.qa.http.ResponseMapper;

import java.util.List;

/** Typed facade over {@link PlayerFeignClient}, bound to one bearer token. */
public final class PlayersApi {

    /** Status returned by create; matches the documented 201. */
    public static final int CREATE_STATUS = 201;
    /** Status returned by getOne. The document declares 201 for this read, and the service agrees. */
    public static final int GET_ONE_STATUS = 201;

    private static final TypeReference<PlayerResponseDto> PLAYER = new TypeReference<>() {
    };
    private static final TypeReference<List<PlayerResponseDto>> PLAYER_LIST = new TypeReference<>() {
    };

    private final PlayerFeignClient client;

    private PlayersApi(PlayerFeignClient client) {
        this.client = client;
    }

    public static PlayersApi withToken(String authorizationHeaderValue) {
        return new PlayersApi(FeignClients.createAuthorized(PlayerFeignClient.class, authorizationHeaderValue));
    }

    /** No {@code Authorization} header — for the negative security checks. */
    public static PlayersApi anonymous() {
        return new PlayersApi(FeignClients.create(PlayerFeignClient.class));
    }

    public ApiResponse<PlayerResponseDto> create(PlayerRequestDto player) {
        return ResponseMapper.map(client.create(player), PLAYER);
    }

    public ApiResponse<PlayerResponseDto> getOne(String email) {
        return ResponseMapper.map(client.getOne(new PlayerRequestOneDto(email)), PLAYER);
    }

    public ApiResponse<List<PlayerResponseDto>> getAll() {
        return ResponseMapper.map(client.getAll(), PLAYER_LIST);
    }

    public ApiResponse<PlayerResponseDto> deleteOne(String id) {
        return ResponseMapper.map(client.deleteOne(id), PLAYER);
    }
}
