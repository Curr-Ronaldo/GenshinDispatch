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
        byte[] customConfig = "{\"sdkenv\":\"2\",\"checkdevice\":\"false\",\"loadPatch\":\"false\",\"showexception\":\"false\",\"regionConfig\":\"pm|fk|add\",\"downloadMode\":\"0\"}".getBytes();
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
                                    .setDataUrl("http://out.coure.cn/client_design_data/3.4_live")
                                    .setResourceUrl("http://out.coure.cn/client_game_res/3.4_live")
                                    .setClientDataVersion(13021296)
                                    .setClientSilenceDataVersion(12901190)
                                    .setClientVersionSuffix("8f79734b55")
                                    .setClientSilenceVersionSuffix("326dbbbf65")
                                    .setClientDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"1eeeff672e899799d05b721c33781d5f\", \"fileSize\": 5147}")
                                    .setClientSilenceDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"59bd72d68bdc13d7269339c1765f8b86\", \"fileSize\": 410}")
                                    .setResVersionConfig(ResVersionConfigOuterClass.ResVersionConfig.newBuilder()
                                            .setRelogin(false)
                                            .setVersion(12591909)
                                            .setVersionSuffix("58182dc06f")
                                            .setBranch("3.4_live")
                                            .setReleaseTotalSize("0")
                                            .setMd5("""
                                                    {\"remoteName\": \"res_versions_external\", \"md5\": \"2b6a84f0a36e1d7624ea2e2d96d51b9a\", \"fileSize\": 809789}\r\n{\"remoteName\": \"res_versions_medium\", \"md5\": \"a929eb9155bacfd404c09c1916e6177e\", \"fileSize\": 328148}\r\n{\"remoteName\": \"res_versions_streaming\", \"md5\": \"8f934c499a9c707fa3f02eb8b01ab642\", \"fileSize\": 78927}\r\n{\"remoteName\": \"release_res_versions_external\", \"md5\": \"7f679c2550072874c65010f47ce0ee55\", \"fileSize\": 809789}\r\n{\"remoteName\": \"release_res_versions_medium\", \"md5\": \"e0a8033e2b30a1c99980ac2fc1ae4afd\", \"fileSize\": 328148}\r\n{\"remoteName\": \"release_res_versions_streaming\", \"md5\": \"4ede994c13a3fdd54cec38032c2cd31d\", \"fileSize\": 78927}\r\n{\"remoteName\": \"base_revision\", \"md5\": \"d41d8cd98f00b204e9800998ecf8427e\", \"fileSize\": 0}
                                                    """)
                                            .build())
                                    .build())
                            .setRegionCustomConfigEncrypted(ByteString.copyFrom(customConfig))
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
                                    .setDataUrl("http://out.coure.cn/client_design_data/3.4_live")
                                    .setResourceUrl("http://out.coure.cn/client_game_res/3.4_live")
                                    .setClientDataVersion(13021296)
                                    .setClientSilenceDataVersion(12901190)
                                    .setClientVersionSuffix("8f79734b55")
                                    .setClientSilenceVersionSuffix("326dbbbf65")
                                    .setClientDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"1eeeff672e899799d05b721c33781d5f\", \"fileSize\": 5147}")
                                    .setClientSilenceDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"59bd72d68bdc13d7269339c1765f8b86\", \"fileSize\": 410}")
                                    .setResVersionConfig(ResVersionConfigOuterClass.ResVersionConfig.newBuilder()
                                            .setRelogin(false)
                                            .setVersion(12591909)
                                            .setVersionSuffix("58182dc06f")
                                            .setBranch("3.4_live")
                                            .setReleaseTotalSize("0")
                                            .setMd5("""
                                                {\"remoteName\": \"res_versions_external\", \"md5\": \"89f1405455bfd76984eaa67aa88ae079\", \"fileSize\": 402215}\r\n{\"remoteName\": \"res_versions_medium\", \"md5\": \"e54c06341410a79d0dc8507514381df7\", \"fileSize\": 109447}\r\n{\"remoteName\": \"res_versions_streaming\", \"md5\": \"d7295dd875cbf760af85192f5de3fac2\", \"fileSize\": 29760}\r\n{\"remoteName\": \"release_res_versions_external\", \"md5\": \"1de0240586dbc96cdbab9605f2a09bb6\", \"fileSize\": 402215}\r\n{\"remoteName\": \"release_res_versions_medium\", \"md5\": \"968540dfb2efd3911ec2cac9ba9ffc20\", \"fileSize\": 109447}\r\n{\"remoteName\": \"release_res_versions_streaming\", \"md5\": \"ef5181f5543be84ed2cccd468a4b20e1\", \"fileSize\": 29760}\r\n{\"remoteName\": \"base_revision\", \"md5\": \"d41d8cd98f00b204e9800998ecf8427e\", \"fileSize\": 0}
                                                """)

                                            .build())
                                    .build())
                            .setRegionCustomConfigEncrypted(ByteString.copyFrom(customConfig))
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
                                    .setDataUrl("http://out.coure.cn/client_design_data/3.4_live")
                                    .setResourceUrl("http://out.coure.cn/client_game_res/3.4_live")
                                    .setClientDataVersion(13021296)
                                    .setClientSilenceDataVersion(12901190)
                                    .setClientVersionSuffix("8f79734b55")
                                    .setClientSilenceVersionSuffix("326dbbbf65")
                                    .setClientDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"1eeeff672e899799d05b721c33781d5f\", \"fileSize\": 5147}")
                                    .setClientSilenceDataMd5("{\"remoteName\": \"data_versions\", \"md5\": \"59bd72d68bdc13d7269339c1765f8b86\", \"fileSize\": 410}")
                                    .setResVersionConfig(ResVersionConfigOuterClass.ResVersionConfig.newBuilder()
                                            .setRelogin(false)
                                            .setVersion(12591909)
                                            .setVersionSuffix("58182dc06f")
                                            .setBranch("3.4_live")
                                            .setReleaseTotalSize("0")
                                            .setMd5("""
                                                    {\"remoteName\": \"res_versions_external\", \"md5\": \"1ec457f029cd36a01e5bfd57daf7cf6d\", \"fileSize\": 408267}\r\n{\"remoteName\": \"res_versions_medium\", \"md5\": \"65579c93b2b35c4f5ba784137976e8a0\", \"fileSize\": 97504}\r\n{\"remoteName\": \"res_versions_streaming\", \"md5\": \"703dfabb7630d666c8305f469afe4114\", \"fileSize\": 2402}\r\n{\"remoteName\": \"release_res_versions_external\", \"md5\": \"2ade8cdb33c14d5774ae28640798e4ad\", \"fileSize\": 408267}\r\n{\"remoteName\": \"release_res_versions_medium\", \"md5\": \"b1a0edc7e7b18ca4d4dfa8bee586bf5b\", \"fileSize\": 97504}\r\n{\"remoteName\": \"release_res_versions_streaming\", \"md5\": \"6b45265d8e929e32b3ddce80fb4ab8e9\", \"fileSize\": 2402}\r\n{\"remoteName\": \"base_revision\", \"md5\": \"d41d8cd98f00b204e9800998ecf8427e\", \"fileSize\": 0}
                                                    """)
                                            .build())
                                    .build())
                            .setRegionCustomConfigEncrypted(ByteString.copyFrom(customConfig))
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
                cipher.init(Cipher.ENCRYPT_MODE, Objects.equals(key_id, "5") ? Crypto.CUR_OS_ENCRYPT_KEY : Crypto.CUR_CN_ENCRYPT_KEY);

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