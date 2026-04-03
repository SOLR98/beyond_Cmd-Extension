package com.solr98.beyondcmdextension.handler;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络传输管理器
 * 管理跨网络物品传输请求和确认机制
 */
public class NetworkTransferManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 传输请求超时时间（毫秒）
    private static final long REQUEST_TIMEOUT = 5 * 60 * 1000; // 5分钟
    
    /**
     * 传输资源类型枚举
     */
    public enum TransferResourceType {
        ITEM,    // 物品
        FLUID,   // 流体
        ENERGY   // 能量
    }
    
    /**
     * 传输请求类 - 支持物品、流体、能量
     */
    public static class TransferRequest {
        private final UUID requestId;
        private final ServerPlayer requester;
        private final ServerPlayer targetPlayer;
        private final int sourceNetId;
        private final int targetNetId;
        private final TransferResourceType resourceType;
        private final ItemStack item;           // 物品类型时使用
        private final FluidStack fluid;         // 流体类型时使用
        private final String energyType;        // 能量类型时使用
        private final long amount;              // 传输数量（使用long上限）
        private final long requestTime;
        private TransferStatus status;
        
        // 物品传输构造函数
        public TransferRequest(ServerPlayer requester, ServerPlayer targetPlayer, 
                              int sourceNetId, int targetNetId, ItemStack item, long amount) {
            this.requestId = UUID.randomUUID();
            this.requester = requester;
            this.targetPlayer = targetPlayer;
            this.sourceNetId = sourceNetId;
            this.targetNetId = targetNetId;
            this.resourceType = TransferResourceType.ITEM;
            this.item = item.copy();
            this.fluid = null;
            this.energyType = null;
            this.amount = amount;
            this.requestTime = System.currentTimeMillis();
            this.status = TransferStatus.PENDING;
        }
        
        // 流体传输构造函数
        public TransferRequest(ServerPlayer requester, ServerPlayer targetPlayer,
                              int sourceNetId, int targetNetId, FluidStack fluid, long amount) {
            this.requestId = UUID.randomUUID();
            this.requester = requester;
            this.targetPlayer = targetPlayer;
            this.sourceNetId = sourceNetId;
            this.targetNetId = targetNetId;
            this.resourceType = TransferResourceType.FLUID;
            this.item = null;
            this.fluid = fluid.copy();
            this.energyType = null;
            this.amount = amount;
            this.requestTime = System.currentTimeMillis();
            this.status = TransferStatus.PENDING;
        }
        
        // 能量传输构造函数
        public TransferRequest(ServerPlayer requester, ServerPlayer targetPlayer,
                              int sourceNetId, int targetNetId, String energyType, long amount) {
            this.requestId = UUID.randomUUID();
            this.requester = requester;
            this.targetPlayer = targetPlayer;
            this.sourceNetId = sourceNetId;
            this.targetNetId = targetNetId;
            this.resourceType = TransferResourceType.ENERGY;
            this.item = null;
            this.fluid = null;
            this.energyType = energyType;
            this.amount = amount;
            this.requestTime = System.currentTimeMillis();
            this.status = TransferStatus.PENDING;
        }
        
        public UUID getRequestId() {
            return requestId;
        }
        
        public ServerPlayer getRequester() {
            return requester;
        }
        
        public ServerPlayer getTargetPlayer() {
            return targetPlayer;
        }
        
        public int getSourceNetId() {
            return sourceNetId;
        }
        
        public int getTargetNetId() {
            return targetNetId;
        }
        
        public TransferResourceType getResourceType() {
            return resourceType;
        }
        
        public ItemStack getItem() {
            return item != null ? item.copy() : null;
        }
        
        public FluidStack getFluid() {
            return fluid != null ? fluid.copy() : null;
        }
        
        public String getEnergyType() {
            return energyType;
        }
        
        public long getAmount() {
            return amount;
        }
        
        public long getRequestTime() {
            return requestTime;
        }
        
        public TransferStatus getStatus() {
            return status;
        }
        
        public void setStatus(TransferStatus status) {
            this.status = status;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - requestTime > REQUEST_TIMEOUT;
        }
        
        @Override
        public String toString() {
            String resourceInfo;
            switch (resourceType) {
                case ITEM:
                    resourceInfo = String.format("item=%s, amount=%d", 
                            item != null ? item.getDescriptionId() : "null", amount);
                    break;
                case FLUID:
                    resourceInfo = String.format("fluid=%s, amount=%d", 
                            fluid != null ? fluid.getFluid().getFluidType().getDescriptionId() : "null", amount);
                    break;
                case ENERGY:
                    resourceInfo = String.format("energy=%s, amount=%d", energyType, amount);
                    break;
                default:
                    resourceInfo = "unknown resource";
            }
            
            return String.format("TransferRequest{id=%s, requester=%s, target=%s, sourceNet=%d, targetNet=%d, type=%s, %s, status=%s}",
                    requestId, requester.getGameProfile().getName(), targetPlayer.getGameProfile().getName(),
                    sourceNetId, targetNetId, resourceType, resourceInfo, status);
        }
        
        /**
         * 获取资源描述（用于显示）
         */
        public String getResourceDescription() {
            switch (resourceType) {
                case ITEM:
                    return item != null ? item.getDescriptionId() : "未知物品";
                case FLUID:
                    return fluid != null ? fluid.getDisplayName().getString() : "未知流体";
                case ENERGY:
                    return energyType != null ? energyType : "未知能量";
                default:
                    return "未知资源";
            }
        }
    }
    
    /**
     * 传输状态枚举
     */
    public enum TransferStatus {
        PENDING,    // 等待确认
        ACCEPTED,   // 已接受
        DENIED,     // 已拒绝
        TIMEOUT,    // 超时
        CANCELLED   // 已取消
    }
    
    // 存储所有传输请求
    private static final Map<UUID, TransferRequest> transferRequests = new ConcurrentHashMap<>();
    // 按目标玩家存储请求ID
    private static final Map<UUID, UUID> playerPendingRequests = new ConcurrentHashMap<>();
    
    /**
     * 创建新的物品传输请求
     */
    public static TransferRequest createItemRequest(ServerPlayer requester, ServerPlayer targetPlayer,
                                                   int sourceNetId, int targetNetId, ItemStack item, long amount) {
        return createRequestInternal(requester, targetPlayer, sourceNetId, targetNetId, 
                                    new TransferRequest(requester, targetPlayer, sourceNetId, targetNetId, item, amount));
    }
    
    /**
     * 创建新的流体传输请求
     */
    public static TransferRequest createFluidRequest(ServerPlayer requester, ServerPlayer targetPlayer,
                                                    int sourceNetId, int targetNetId, FluidStack fluid, long amount) {
        return createRequestInternal(requester, targetPlayer, sourceNetId, targetNetId,
                                    new TransferRequest(requester, targetPlayer, sourceNetId, targetNetId, fluid, amount));
    }
    
    /**
     * 创建新的能量传输请求
     */
    public static TransferRequest createEnergyRequest(ServerPlayer requester, ServerPlayer targetPlayer,
                                                     int sourceNetId, int targetNetId, String energyType, long amount) {
        return createRequestInternal(requester, targetPlayer, sourceNetId, targetNetId,
                                    new TransferRequest(requester, targetPlayer, sourceNetId, targetNetId, energyType, amount));
    }
    
    /**
     * 内部创建请求方法
     */
    private static TransferRequest createRequestInternal(ServerPlayer requester, ServerPlayer targetPlayer,
                                                        int sourceNetId, int targetNetId, TransferRequest request) {
        // 清理过期请求
        cleanupExpiredRequests();
        
        // 检查目标玩家是否已有待处理请求
        UUID existingRequestId = playerPendingRequests.get(targetPlayer.getUUID());
        if (existingRequestId != null) {
            TransferRequest existingRequest = transferRequests.get(existingRequestId);
            if (existingRequest != null && existingRequest.getStatus() == TransferStatus.PENDING) {
                // 目标玩家已有待处理请求，取消旧请求
                existingRequest.setStatus(TransferStatus.CANCELLED);
                LOGGER.debug("Cancelled existing pending request for player {}", targetPlayer.getGameProfile().getName());
            }
        }
        
        // 存储新请求
        transferRequests.put(request.getRequestId(), request);
        playerPendingRequests.put(targetPlayer.getUUID(), request.getRequestId());
        
        LOGGER.info("Created transfer request: {}", request);
        return request;
    }
    
    /**
     * 获取玩家的待处理请求
     */
    public static TransferRequest getPendingRequest(ServerPlayer player) {
        UUID requestId = playerPendingRequests.get(player.getUUID());
        if (requestId == null) {
            return null;
        }
        
        TransferRequest request = transferRequests.get(requestId);
        if (request == null || request.getStatus() != TransferStatus.PENDING || request.isExpired()) {
            // 请求不存在、已处理或已过期
            if (request != null && request.isExpired()) {
                request.setStatus(TransferStatus.TIMEOUT);
                LOGGER.debug("Request {} expired", requestId);
            }
            playerPendingRequests.remove(player.getUUID());
            return null;
        }
        
        return request;
    }
    
    /**
     * 接受传输请求
     */
    public static boolean acceptRequest(ServerPlayer player) {
        TransferRequest request = getPendingRequest(player);
        if (request == null) {
            return false;
        }
        
        request.setStatus(TransferStatus.ACCEPTED);
        playerPendingRequests.remove(player.getUUID());
        LOGGER.info("Transfer request accepted: {}", request.getRequestId());
        return true;
    }
    
    /**
     * 拒绝传输请求
     */
    public static boolean denyRequest(ServerPlayer player) {
        TransferRequest request = getPendingRequest(player);
        if (request == null) {
            return false;
        }
        
        request.setStatus(TransferStatus.DENIED);
        playerPendingRequests.remove(player.getUUID());
        LOGGER.info("Transfer request denied: {}", request.getRequestId());
        return true;
    }
    
    /**
     * 取消传输请求
     */
    public static boolean cancelRequest(ServerPlayer requester) {
        // 查找由该玩家发起的待处理请求
        for (TransferRequest request : transferRequests.values()) {
            if (request.getRequester().getUUID().equals(requester.getUUID()) && 
                request.getStatus() == TransferStatus.PENDING) {
                request.setStatus(TransferStatus.CANCELLED);
                playerPendingRequests.remove(request.getTargetPlayer().getUUID());
                LOGGER.info("Transfer request cancelled by requester: {}", request.getRequestId());
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取已接受的请求（用于执行传输）
     */
    public static TransferRequest getAcceptedRequest(UUID requestId) {
        TransferRequest request = transferRequests.get(requestId);
        if (request != null && request.getStatus() == TransferStatus.ACCEPTED) {
            return request;
        }
        return null;
    }
    
    /**
     * 完成请求处理（传输完成后调用）
     */
    public static void completeRequest(UUID requestId) {
        TransferRequest request = transferRequests.remove(requestId);
        if (request != null) {
            playerPendingRequests.remove(request.getTargetPlayer().getUUID());
            LOGGER.debug("Completed transfer request: {}", requestId);
        }
    }
    
    /**
     * 清理过期请求
     */
    private static void cleanupExpiredRequests() {
        Iterator<Map.Entry<UUID, TransferRequest>> iterator = transferRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TransferRequest> entry = iterator.next();
            TransferRequest request = entry.getValue();
            
            if (request.isExpired() && request.getStatus() == TransferStatus.PENDING) {
                request.setStatus(TransferStatus.TIMEOUT);
                playerPendingRequests.remove(request.getTargetPlayer().getUUID());
                LOGGER.debug("Cleaned up expired request: {}", entry.getKey());
            }
            
            // 清理已完成的请求（保留一段时间用于调试）
            if (System.currentTimeMillis() - request.getRequestTime() > REQUEST_TIMEOUT * 2) {
                iterator.remove();
                playerPendingRequests.remove(request.getTargetPlayer().getUUID());
            }
        }
    }
    
    /**
     * 获取所有传输请求（用于调试）
     */
    public static Collection<TransferRequest> getAllRequests() {
        return transferRequests.values();
    }
    
    /**
     * 获取玩家发起的请求数量
     */
    public static int getRequestCountByPlayer(ServerPlayer player) {
        int count = 0;
        for (TransferRequest request : transferRequests.values()) {
            if (request.getRequester().getUUID().equals(player.getUUID())) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 获取目标玩家的请求数量
     */
    public static int getTargetRequestCount(ServerPlayer player) {
        int count = 0;
        for (TransferRequest request : transferRequests.values()) {
            if (request.getTargetPlayer().getUUID().equals(player.getUUID())) {
                count++;
            }
        }
        return count;
    }


}