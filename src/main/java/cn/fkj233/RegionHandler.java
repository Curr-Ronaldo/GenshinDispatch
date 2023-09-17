package cn.fkj233;


import cn.fkj233.proto.QueryCurrRegionHttpRspOuterClass.QueryCurrRegionHttpRsp;
import cn.fkj233.proto.QueryRegionListHttpRspOuterClass.QueryRegionListHttpRsp;
import cn.fkj233.proto.RegionInfoOuterClass.RegionInfo;
import cn.fkj233.proto.RegionSimpleInfoOuterClass.RegionSimpleInfo;
import cn.fkj233.proto.ResVersionConfigOuterClass;
import cn.fkj233.proto.StopServerInfoOuterClass.StopServerInfo;
import cn.fkj233.proto.ForceUpdateInfoOuterClass.ForceUpdateInfo;
import com.google.protobuf.ByteString;
import io.javalin.Javalin;
import io.javalin.http.Context;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


import static cn.fkj233.GenshinDispatch.config;
import static cn.fkj233.GenshinDispatch.logger;

/**
 * Handles requests related to region queries.
 */
public final class RegionHandler implements Router {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String lr(String left, String right) {
        return left.isEmpty() ? right : left;
    }
    public static int lr(int left, int right) {
        return left == 0 ? right : left;
    }

    public RegionHandler() {}

    @Override public void applyRoutes(Javalin javalin) {
        javalin.get("/query_region_list", RegionHandler::queryRegionList);
        javalin.get("/query_cur_region/{region}", RegionHandler::queryCurrentRegion );
    }

    /**
     * @route /query_region_list
     */
    private static void queryRegionList(Context ctx) {
        String dispatchDomain = "http" + (config.useEncryption ? "s" : "") + "://"
                + lr(config.accessAddress, config.bindAddress) + ":"
                + lr(config.accessPort, config.bindPort);

        List<RegionSimpleInfo> servers = new ArrayList<>();
        List<String> usedNames = new ArrayList<>();

        var configuredRegions = new ArrayList<>(List.of(config.regions));

        configuredRegions.forEach(region -> {
            if (usedNames.contains(region.Name)) {
                logger.error("Region name already in use.");
                return;
            }

            var identifier = RegionSimpleInfo.newBuilder()
                    .setName(region.Name).setTitle(region.Title).setType("DEV_PUBLIC")
                    .setDispatchUrl(dispatchDomain + "/query_cur_region/" + region.Name)
                    .build();
            usedNames.add(region.Name);
            servers.add(identifier);
        });

        byte[] customConfig = "{\"sdkenv\":\"2\",\"checkdevice\":\"false\",\"loadPatch\":\"false\",\"showexception\":\"false\",\"regionConfig\":\"pm|fk|add\",\"downloadMode\":\"0\"}".getBytes();
        Crypto.xor(customConfig, Crypto.DISPATCH_KEY);

        QueryRegionListHttpRsp updatedRegionList = QueryRegionListHttpRsp.newBuilder()
                .addAllRegionList(servers)
                .setClientSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                .setClientCustomConfigEncrypted(ByteString.copyFrom(customConfig))
                .setEnableLoginPc(true).build();

        ctx.result(Base64.getEncoder().encodeToString(updatedRegionList.toByteString().toByteArray()));

        logger.info(String.format("[Dispatch] Client %s request: query_region_list", ctx.ip()));
    }

    private static QueryCurrRegionHttpRsp getCurrRegion(String key, String version) {
        Config.Region region = null;
        for (var data : config.regions) {
            if (data.Name.equals(key)) {
                region = data;
                break;
            }
        }

        if (region == null) return null;

        var regionInfo = RegionInfo.newBuilder()
                .setGateserverIp(region.Ip)
                .setGateserverPort(region.Port)
                .setSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                .build();
        QueryCurrRegionHttpRsp.Builder updatedQuery = QueryCurrRegionHttpRsp.newBuilder().setRegionInfo(regionInfo);
        if (region.Run && version != null) {
            String versionCode = version;
            //logger.info(versionCode);
            switch (versionCode) {
                case "CNRELWin3.4.0":
                    updatedQuery
                            .setRetcode(0)
                            .setRegionInfo(RegionInfo.newBuilder()
                                    .setGateserverIp(region.Ip)
                                    .setGateserverPort(region.Port)
                                    .setSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                                    .setDataUrlBak("3.4_live")
                                    .setResourceUrlBak("3.4_live")
                                    .setDataUrl("https://update.vme50.icu/client_design_data/3.4_live")
                                    .setResourceUrl("https://update.vme50.icu/client_game_res/3.4_live")
                                    .setClientDataVersion(13021296)
                                    .setClientSilenceDataVersion(12901190)
                                    .setClientVersionSuffix("8f79734b55")
                                    .setClientSilenceVersionSuffix("326dbbbf65")
                                    .setClientDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"1eeeff672e899799d05b721c33781d5f\", \"fileSize\": 5147}")
                                    .setClientSilenceDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"59bd72d68bdc13d7269339c1765f8b86\", \"fileSize\": 410}")
                                    .setResVersionConfig(ResVersionConfigOuterClass.ResVersionConfig.newBuilder()
                                            .setRelogin(false)
                                            .setVersion(12875869)
                                            .setVersionSuffix("b9da23cec2")
                                            .setBranch("3.4_live")
                                            .setReleaseTotalSize("0")
                                            .setMd5("""
                                                    {\"remoteName\": \"res_versions_external\", \"md5\": \"9cb4f3667ee8105e212cbfa7990acb5c\", \"fileSize\": 788001}\r\n{\"remoteName\": \"res_versions_medium\", \"md5\": \"fbe87049c76cba5e63c9e0e026e6c041\", \"fileSize\": 318424}\r\n{\"remoteName\": \"res_versions_streaming\", \"md5\": \"404d2a3935fcae3017039032d590f272\", \"fileSize\": 78791}\r\n{\"remoteName\": \"release_res_versions_external\", \"md5\": \"8473b82893952f4544c67eb6e8fe6832\", \"fileSize\": 788001}\r\n{\"remoteName\": \"release_res_versions_medium\", \"md5\": \"bab1f2be82928c972a111e40568c1f51\", \"fileSize\": 318424}\r\n{\"remoteName\": \"release_res_versions_streaming\", \"md5\": \"f04f7977bc87779b823ce19a60771287\", \"fileSize\": 78791}\r\n{\"remoteName\": \"base_revision\", \"md5\": \"af0f241d9f231b2d3230e8f707a0c321\", \"fileSize\": 19}
                                                    """)
                                            .build())
                                    .build())
                            .setClientSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED));
                    break;
                case "OSRELiOS3.4.0":
                    updatedQuery
                            .setRetcode(0)
                            .setRegionInfo(RegionInfo.newBuilder()
                                    .setGateserverIp(region.Ip)
                                    .setGateserverPort(region.Port)
                                    .setSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                                    .setDataUrlBak("3.4_live")
                                    .setResourceUrlBak("3.4_live")
                                    .setDataUrl("https://update.vme50.icu/client_design_data/3.4_live")
                                    .setResourceUrl("https://update.vme50.icu/client_game_res/3.4_live")
                                    .setClientDataVersion(13021296)
                                    .setClientSilenceDataVersion(12901190)
                                    .setClientVersionSuffix("8f79734b55")
                                    .setClientSilenceVersionSuffix("326dbbbf65")
                                    .setClientDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"1eeeff672e899799d05b721c33781d5f\", \"fileSize\": 5147}")
                                    .setClientSilenceDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"59bd72d68bdc13d7269339c1765f8b86\", \"fileSize\": 410}")
                                    .setResVersionConfig(ResVersionConfigOuterClass.ResVersionConfig.newBuilder()
                                            .setRelogin(false)
                                            .setVersion(12875869)
                                            .setVersionSuffix("b9da23cec2")
                                            .setBranch("3.4_live")
                                            .setReleaseTotalSize("0")
                                            .setMd5("""
                                                    {\"remoteName\": \"res_versions_external\", \"md5\": \"e38e728250f20ae84cf5d475d2c3778a\", \"fileSize\": 383340}\r\n{\"remoteName\": \"res_versions_medium\", \"md5\": \"558b841d70f4c3e1c74fbf6dc30f0702\", \"fileSize\": 99746}\r\n{\"remoteName\": \"res_versions_streaming\", \"md5\": \"13153fbeb959e252e2734bae6dd7ca56\", \"fileSize\": 29539}\r\n{\"remoteName\": \"release_res_versions_external\", \"md5\": \"d6d72bccfc602a9ec3c2a2b591b0f753\", \"fileSize\": 383340}\r\n{\"remoteName\": \"release_res_versions_medium\", \"md5\": \"8383d854d05e557f34a30d58ec0381b2\", \"fileSize\": 99746}\r\n{\"remoteName\": \"release_res_versions_streaming\", \"md5\": \"776b1100e085a2a5f16cb1576e9fdead\", \"fileSize\": 29539}\r\n{\"remoteName\": \"base_revision\", \"md5\": \"af0f241d9f231b2d3230e8f707a0c321\", \"fileSize\": 19}
                                                    """)
                                            .build())
                                    .build())
                            .setClientSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED));
                    break;
                case "OSRELAndroid3.4.0":
                    updatedQuery
                            .setRetcode(0)
                            .setRegionInfo(RegionInfo.newBuilder()
                                    .setGateserverIp(region.Ip)
                                    .setGateserverPort(region.Port)
                                    .setSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                                    .setDataUrlBak("3.4_live")
                                    .setResourceUrlBak("3.4_live")
                                    .setDataUrl("https://update.vme50.icu/client_design_data/3.4_live")
                                    .setResourceUrl("https://update.vme50.icu/client_game_res/3.4_live")
                                    .setClientDataVersion(13021296)
                                    .setClientSilenceDataVersion(12901190)
                                    .setClientVersionSuffix("8f79734b55")
                                    .setClientSilenceVersionSuffix("326dbbbf65")
                                    .setClientDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"1eeeff672e899799d05b721c33781d5f\", \"fileSize\": 5147}")
                                    .setClientSilenceDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"59bd72d68bdc13d7269339c1765f8b86\", \"fileSize\": 410}")
                                    .setResVersionConfig(ResVersionConfigOuterClass.ResVersionConfig.newBuilder()
                                            .setRelogin(false)
                                            .setVersion(12875869)
                                            .setVersionSuffix("b9da23cec2")
                                            .setBranch("3.4_live")
                                            .setReleaseTotalSize("0")
                                            .setMd5("""
                                                    {\"remoteName\": \"res_versions_external\", \"md5\": \"8dfb9e5a33ba557929826248acb4170a\", \"fileSize\": 401244}\r\n{\"remoteName\": \"res_versions_medium\", \"md5\": \"78f6a919f3d11ab1de866dbbfa85f81c\", \"fileSize\": 95660}\r\n{\"remoteName\": \"res_versions_streaming\", \"md5\": \"da868422a159d4bd163effaedd275783\", \"fileSize\": 2215}\r\n{\"remoteName\": \"release_res_versions_external\", \"md5\": \"02da756f10f83380f5be0026ddd0b33b\", \"fileSize\": 401247}\r\n{\"remoteName\": \"release_res_versions_medium\", \"md5\": \"b5c7164922e73aedd4b7ce87e6fe5f5b\", \"fileSize\": 95660}\r\n{\"remoteName\": \"release_res_versions_streaming\", \"md5\": \"04b13f69ddc53afd2c9dc5f43230b919\", \"fileSize\": 2215}\r\n{\"remoteName\": \"base_revision\", \"md5\": \"af0f241d9f231b2d3230e8f707a0c321\", \"fileSize\": 19}
                                                    """)
                                            .build())
                                    .build())
                            .setClientSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED));
                    break;
            }
        } else {
            try {
                updatedQuery
                        .setRetcode(11)
                        .setMsg(region.StopServer.Title)
                        .setStopServer(StopServerInfo.newBuilder()
                                .setStopBeginTime(Math.toIntExact(dateFormat.parse(region.StopServer.StartTime).getTime() / 1000))
                                .setStopEndTime(Math.toIntExact(dateFormat.parse(region.StopServer.StopTime).getTime() / 1000))
                                .setContentMsg(region.StopServer.Msg)
                                .setUrl(region.StopServer.Url)
                                .build()
                        );
            } catch (ParseException e) {
                logger.info("parse time failed.");
            }
        }
        return updatedQuery.build();
    }

    /**
     * @route /query_cur_region/{region}
     */
    private static void queryCurrentRegion(Context ctx) {
        // Get region to query.
        String regionName = ctx.pathParam("region");
        String versionName = ctx.queryParam("version");
        var region = getCurrRegion(regionName, versionName);

        // Get region data.
        byte[] regionData = "CAESGE5vdCBGb3VuZCB2ZXJzaW9uIGNvbmZpZw==".getBytes(StandardCharsets.UTF_8);
        if (ctx.queryParamMap().values().size() > 0) {
            if (region != null)
                regionData = region.toByteString().toByteArray();
        }

        if (versionName == null) {
            ctx.result(regionData);
            return;
        }

        String[] versionCode = versionName.replaceAll(Pattern.compile("[a-zA-Z]").pattern(), "").split("\\.");
        int versionMajor = Integer.parseInt(versionCode[0]);
        int versionMinor = Integer.parseInt(versionCode[1]);
        int versionFix = Integer.parseInt(versionCode[2]);

        if (versionMajor >= 3 || (versionMajor == 2 && versionMinor == 7 && versionFix >= 50) || (versionMajor == 2 && versionMinor == 8)) {
            try {

                if (ctx.queryParam("dispatchSeed") == null) {
                    // More love for UA Patch players
                    var rsp = new QueryCurRegionRspJson();

                    rsp.content = Base64.getEncoder().encodeToString(regionData);
                    rsp.sign = "TW9yZSBsb3ZlIGZvciBVQSBQYXRjaCBwbGF5ZXJz";

                    ctx.json(rsp);
                    return;
                }

                String key_id = ctx.queryParam("key_id");
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, Objects.equals(key_id, "3") ? Crypto.CUR_OS_ENCRYPT_KEY : Crypto.CUR_CN_ENCRYPT_KEY);

                //Encrypt regionInfo in chunks
                ByteArrayOutputStream encryptedRegionInfoStream = new ByteArrayOutputStream();

                //Thank you so much GH Copilot
                int chunkSize = 256 - 11;
                int regionInfoLength = regionData.length;
                int numChunks = (int) Math.ceil(regionInfoLength / (double) chunkSize);

                for (int i = 0; i < numChunks; i++) {
                    byte[] chunk = Arrays.copyOfRange(regionData, i * chunkSize, Math.min((i + 1) * chunkSize, regionInfoLength));
                    byte[] encryptedChunk = cipher.doFinal(chunk);
                    encryptedRegionInfoStream.write(encryptedChunk);
                }

                Signature privateSignature = Signature.getInstance("SHA256withRSA");
                privateSignature.initSign(Crypto.CUR_SIGNING_KEY);
                privateSignature.update(regionData);

                var rsp = new QueryCurRegionRspJson();

                rsp.content = Base64.getEncoder().encodeToString(encryptedRegionInfoStream.toByteArray());
                rsp.sign = Base64.getEncoder().encodeToString(privateSignature.sign());

                ctx.json(rsp);
            }
            catch (Exception e) {
                logger.error("An error occurred while handling query_cur_region.", e);
            }
        }
        else {
            ctx.result(regionData);
        }
        logger.info(String.format("Client %s request: query_cur_region/%s", ctx.ip(), regionName));
    }

    /**
     * Region data container.
     */
    public static class RegionData {
        private final QueryCurrRegionHttpRsp regionQuery;
        private final String base64;

        public RegionData(QueryCurrRegionHttpRsp prq, String b64) {
            this.regionQuery = prq;
            this.base64 = b64;
        }

        public QueryCurrRegionHttpRsp getRegionQuery() {
            return this.regionQuery;
        }

        public String getBase64() {
            return this.base64;
        }
    }
}