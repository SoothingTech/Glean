package com.soothingtech.glean.account.app;

import com.soothingtech.glean.account.api.dto.DeviceView;
import com.soothingtech.glean.account.domain.Device;
import com.soothingtech.glean.account.domain.DeviceRepository;
import com.soothingtech.glean.account.token.JwtService;
import com.soothingtech.glean.account.token.TokenBlacklist;
import com.soothingtech.glean.common.BizException;
import com.soothingtech.glean.common.ErrorCodes;
import com.soothingtech.glean.common.Times;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository devices;
    private final TokenBlacklist blacklist;
    private final AuditService audit;
    private final JwtService jwt;

    public DeviceService(DeviceRepository devices,
                         TokenBlacklist blacklist,
                         AuditService audit,
                         JwtService jwt) {
        this.devices = devices;
        this.blacklist = blacklist;
        this.audit = audit;
        this.jwt = jwt;
    }

    /** 登录时登记/刷新设备，返回设备的规范化 id */
    @Transactional
    public String touch(UUID userId, String deviceId, String platform) {
        Device device = devices.findById(deviceId)
                .filter(existing -> existing.getUserId().equals(userId))
                .orElseGet(() -> new Device(deviceId, userId, platform));
        device.touch(platform);
        devices.save(device);
        return device.getId();
    }

    public List<DeviceView> list(UUID userId) {
        return devices.findByUserIdOrderByLastSeenAtDesc(userId).stream()
                .map(d -> new DeviceView(d.getId(), d.getPlatform(), Times.of(d.getLastSeenAt()), Times.of(d.getRevokedAt())))
                .toList();
    }

    /**
     * 远程下线：设备置 revoked_at，并把该设备的会话水位推到当前时刻，
     * 使其 access 与 refresh 令牌全部立即失效（幂等：重复调用结果一致）。
     */
    @Transactional
    public void revoke(UUID userId, String deviceId) {
        Device device = devices.findById(deviceId)
                .filter(existing -> existing.getUserId().equals(userId))
                .orElseThrow(() -> new BizException(ErrorCodes.FORBIDDEN, 403, "设备不存在或不属于当前用户"));

        device.revoke();
        devices.save(device);
        blacklist.revokeDevice(userId, deviceId, jwt.getRefreshTtl());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("platform", device.getPlatform());
        audit.record(userId, AuditService.ACTION_DEVICE_REVOKE, payload);
    }
}
