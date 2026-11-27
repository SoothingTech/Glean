package com.soothingtech.glean.account.api;

import com.soothingtech.glean.account.api.dto.DeviceView;
import com.soothingtech.glean.account.app.DeviceService;
import com.soothingtech.glean.account.token.AuthPrincipal;
import com.soothingtech.glean.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 设备列表与远程下线（5.7.3.2 第 134—135 行） */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<List<DeviceView>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(deviceService.list(principal.userId()));
    }

    /** 幂等：重复下线同一设备同样返回 204 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable("id") String id) {
        deviceService.revoke(principal.userId(), id);
    }
}
