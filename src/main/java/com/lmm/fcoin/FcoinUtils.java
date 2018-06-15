package com.lmm.fcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class FcoinUtils {

    private static final RetryTemplate retryTemplate = FcoinRetry.getRetryTemplate();

    private static final RetryTemplate tradeRetryTemplate = FcoinRetry.getTradeRetryTemplate();

    private static final Logger logger = LoggerFactory.getLogger(FcoinUtils.class);

    private static final String app_key;
    private static final String app_secret;

    private static final double initMultiple;//初始化平衡的美金
    private static final double maxNum;//单笔最大数量
    private static final double minUsdt;//最小美金
    private static final int pricePrecision;
    private static final int numPrecision;

    private static final int initInterval;//初始化间隔

    static {
        Properties properties = null;
        try {
            properties = PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource("app.properties", FcoinUtils.class.getClassLoader()));
        } catch (IOException e) {
            logger.error("类初始化异常", e);
        }

        app_key = properties.getProperty("app_key");
        app_secret = properties.getProperty("app_secret");

        initMultiple = Double.valueOf(properties.getProperty("initMultiple", "3"));
        maxNum = Double.valueOf(properties.getProperty("maxNum", "1000"));
        minUsdt = Double.valueOf(properties.getProperty("minUsdt", "50"));

        initInterval = Integer.valueOf(properties.getProperty("initInterval", "10"));
        pricePrecision = Integer.valueOf(properties.getProperty("pricePrecision", "2"));
        numPrecision = Integer.valueOf(properties.getProperty("numPrecision", "2"));
    }

    public static BigDecimal getBigDecimal(double value, int scale) {
        return new BigDecimal(value).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal getNum(double b) {
        return getBigDecimal(b, numPrecision);
    }

    public static BigDecimal getMarketPrice(double marketPrice) {
        return getBigDecimal(marketPrice, pricePrecision);
    }

    public static String getSign(String data, String secret) throws Exception {

        String base64_1 = Base64.getEncoder().encodeToString(data.getBytes("utf-8"));
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes("utf-8"), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(base64_1.getBytes("utf-8"));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    public static String getBalance() throws Exception {
        String url = "https://api.fcoin.com/v2/accounts/balance";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        return response.getBody();
    }

    public static void buy(String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        BigDecimal maxNumDeci = getNum(maxNum);
        while (amount.doubleValue() > 0) {
            if (amount.compareTo(maxNumDeci) > 0) {
                subBuy(maxNumDeci.toString(), marketPrice.toString(), symbol, type);
            } else {
                subBuy(amount.toString(), marketPrice.toString(), symbol, type);
                break;
            }
            amount = amount.subtract(maxNumDeci);

            Thread.sleep(5000);
        }

    }

    public static void sell(String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        BigDecimal maxNumDeci = getNum(maxNum);
        while (amount.doubleValue() > 0) {
            if (amount.compareTo(maxNumDeci) > 0) {
                subSell(maxNumDeci.toString(), marketPrice.toString(), symbol, type);
            } else {
                subSell(amount.toString(), marketPrice.toString(), symbol, type);
                break;
            }
            amount = amount.subtract(maxNumDeci);

            Thread.sleep(5000);
        }
    }

    public static void buyNotLimit(String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        subBuy(amount.toString(), marketPrice.toString(), symbol, type);
    }

    public static void sellNotLimit(String symbol, String type, BigDecimal amount, BigDecimal marketPrice) throws Exception {
        subSell(amount.toString(), marketPrice.toString(), symbol, type);
    }

    private static boolean createOrder(String amount, String price, String side, String symbol, String type) throws Exception {
        String url = "https://api.fcoin.com/v2/orders";
        Long timeStamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        MediaType t = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(t);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        //  封装参数，千万不要替换为Map与HashMap，否则参数无法传递
        Map<String, String> params = new HashMap<>();
        //  也支持中文
        params.put("amount", amount);
        params.put("side", side);
        params.put("symbol", symbol);
        params.put("type", type);
        String urlSeri = "";
        if ("limit".equals(type)) {
            urlSeri = "amount=" + amount + "&price=" + price + "&side=" + side + "&symbol=" + symbol + "&type=" + type;
            params.put("price", price);
        } else if ("market".equals(type)) {
            urlSeri = "amount=" + amount + "&side=" + side + "&symbol=" + symbol + "&type=" + type;
        }
        headers.add("FC-ACCESS-SIGNATURE",
                getSign("POST" + url + timeStamp + urlSeri, app_secret));

        String param = JSON.toJSONString(params);
        logger.info(param);
        HttpEntity<String> requestEntity = new HttpEntity<String>(param, headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, requestEntity, String.class);
        logger.info(response.getBody());
        if (StringUtils.isEmpty(response.getBody())) {
            throw new Exception("订单创建失败：" + type);
        }
        if (StringUtils.isEmpty(response.getBody())) {
            String data = JSON.parseObject(response.getBody()).getString("data");
            if (StringUtils.isEmpty(data)) {
                throw new Exception("订单创建失败：" + type);
            }
        }
        return true;
    }

    public static void subSell(String amount, String price, String symbol, String type) throws Exception {
        tradeRetryTemplate.execute(retryContext ->
                createOrder(amount, price, "sell", symbol, type)
        );
    }

    public static void subBuy(String amount, String price, String symbol, String type) throws Exception {
        tradeRetryTemplate.execute(retryContext ->
                createOrder(amount, price, "buy", symbol, type)
        );
    }

    public static Map<String, Double> getPriceInfo(String symbol) throws Exception {
        String url = "https://api.fcoin.com/v2/market/ticker/" + symbol;
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        JSONObject jsonObject = JSON.parseObject(response.getBody());
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("ticker");
        Map<String, Double> result = new HashMap<>();
        double marketPrice = Double.valueOf(jsonArray.get(0).toString());

        result.put("marketPrice", marketPrice);
        double hight_24H = Double.valueOf(jsonArray.get(7).toString());
        double low_24H = Double.valueOf(jsonArray.get(8).toString());

        result.put("24HPrice", (hight_24H + low_24H) / 2);

        return result;
    }

    public static String getSymbols() throws Exception {
        String url = "https://api.fcoin.com/v2/public/symbols";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        return response.getBody();
    }

    public List<String> getOrdes(String symbol, String states, String after, String limit) throws Exception {
        String url = "https://api.fcoin.com/v2/orders?after=" + after + "&limit=" + limit + "&states=" + states + "&symbol=" + symbol;
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY", app_key);
        headers.add("FC-ACCESS-SIGNATURE", getSign("GET" + url + timeStamp, app_secret));
        headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        JSONArray jsonArray = JSON.parseObject(response.getBody()).getJSONArray("data");
        if (jsonArray == null || jsonArray.size() == 0) {
            return new ArrayList<>();
        }
        return jsonArray.stream().map(jsonObject -> ((JSONObject) jsonObject).getString("id")).collect(Collectors.toList());
    }

    public List<String> getNotTradeOrders(String symbol, String after, String limit) throws Exception {
        List<String> list1 = getOrdes(symbol, "submitted", after, limit);
        List<String> list2 = getOrdes(symbol, "partial_filled", after, limit);
        list1.addAll(list2);
        return list1;
    }

    public boolean cancelOrders(List<String> orderIds) throws Exception {
        if (orderIds == null || orderIds.size() == 0) {
            return false;
        }
        String urlPath = "https://api.fcoin.com/v2/orders/%s/submit-cancel";
        for (String orderId : orderIds) {
            retryTemplate.execute(retryContext -> {
                String url = String.format(urlPath, orderId);
                Long timeStamp = System.currentTimeMillis();
                HttpHeaders headers = new HttpHeaders();
                headers.add("FC-ACCESS-KEY", app_key);
                headers.add("FC-ACCESS-TIMESTAMP", timeStamp.toString());
                try {
                    headers.add("FC-ACCESS-SIGNATURE",
                            getSign("POST" + url + timeStamp, app_secret));
                } catch (Exception e) {
                    logger.error(e.toString());
                }
                MediaType t = MediaType.parseMediaType("application/json; charset=UTF-8");
                headers.setContentType(t);
                headers.setAccept(Collections.singletonList(MediaType.ALL));

                HttpEntity<String> requestEntity = new HttpEntity<String>(null, headers);
                RestTemplate client = new RestTemplate();
                client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
                ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, requestEntity, String.class);
                if (StringUtils.isEmpty(response.getBody())) {
                    throw new Exception("cacel order error");
                }
                boolean flag = JSON.parseObject(response.getBody()).getBoolean("data");
                if (!flag) {
                    throw new Exception("cacel order error");
                }
                return true;
            });
        }
        return true;
    }

    private Map<String, Balance> buildBalance(String balance) {
        Map<String, Balance> map = new HashMap<>();

        JSONObject jsonObject = JSON.parseObject(balance);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonArray.stream().forEach(jsonObj -> {
            JSONObject obj = (JSONObject) jsonObj;
            Balance balanceVo = new Balance();
            balanceVo.setAvailable(Double.valueOf(obj.getString("available")));
            balanceVo.setBalance(Double.valueOf(obj.getString("balance")));
            balanceVo.setFrozen(Double.valueOf(obj.getString("frozen")));
            map.put(obj.getString("currency"), balanceVo);
        });

        return map;
    }

    private boolean isHaveInitBuyAndSell(double ft, double usdt, double marketPrice, double initUsdt, String symbol, String type) throws Exception {
        //初始化小的
        double ftValue = ft * marketPrice;
        if (ftValue < usdt && Math.abs(ftValue - usdt) > 0.1 * (ftValue + usdt)) {
            //买ft
            double num = Math.min((usdt - ftValue) / 2, initUsdt);
            BigDecimal b = getNum(num / marketPrice);//现价的数量都为ft的数量
            try {
                buy(symbol, type, b, getMarketPrice(marketPrice));//此处不需要重试，让上次去判断余额后重新平衡
            } catch (Exception e) {
                logger.error("初始化买有异常发生", e);
                throw new Exception(e);
            }

        } else if (usdt < ftValue && Math.abs(ftValue - usdt) > 0.1 * (ftValue + usdt)) {
            //卖ft
            double num = Math.min((ftValue - usdt) / 2, initUsdt);
            BigDecimal b = getBigDecimal(num / marketPrice, 2);
            try {
                sell(symbol, type, b, getMarketPrice(marketPrice));//此处不需要重试，让上次去判断余额后重新平衡
            } catch (Exception e) {
                logger.error("初始化卖有异常发生", e);
                throw new Exception(e);
            }
        } else {
            return false;
        }

        Thread.sleep(3000);
        return true;
    }

    /**
     * 自买自卖，但是增加挂单超时取消功能
     *
     * @param symbol   交易对
     * @param ftName   交易币名称
     * @param usdtName 市场币名称
     * @throws Exception
     */
    public void ftusdt(String symbol, String ftName, String usdtName) throws Exception {
        int tradeCount = 0;
        int frozenCount = 0;
        while (true) {

            //查询余额
            String balance = null;
            try {
                balance = retryTemplate.execute(retryContext ->
                        getBalance()
                );
            } catch (Exception e) {
                logger.error("==========fcoinUtils.getBalance重试后还是异常============", e);
                continue;
            }

            Map<String, Balance> balances = buildBalance(balance);
            Balance ftBalance = balances.get(ftName);
            Balance usdtBalance = balances.get(usdtName);

            double ft = ftBalance.getBalance();
            double usdt = usdtBalance.getBalance();
            //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
            if (ftBalance.getFrozen() > 0.099 * ft || usdtBalance.getFrozen() > 0.099 * usdt) {
                frozenCount++;
                if (frozenCount % 40 == 0) {
                    cancelOrders(getNotTradeOrders(symbol, "0", "100"));
                }
                Thread.sleep(3000);
                continue;
            }

            logger.info("===============balance: usdt:{},ft:{}========================", usdt, ft);
            Map<String, Double> priceInfo = getPriceInfo(symbol);
            Double marketPrice = priceInfo.get("marketPrice");
            //usdt小于51并且ft的价值小于51
            if ((usdt < (minUsdt + 1) && ft < ((minUsdt + 1) / marketPrice))
                    || (usdt < (minUsdt + 1) && Math.abs(ft * marketPrice - usdt) < 11)
                    || (ft < ((minUsdt + 1) / marketPrice) && Math.abs(ft * marketPrice - usdt) < 11)) {
                logger.info("跳出循环，ustd:{}, marketPrice:{}", usdt, marketPrice);
                break;
            }

            //ft:usdt=1:0.6 平衡资金
            double ftValue = ft * marketPrice;
            double initUsdt = maxNum * initMultiple * marketPrice;
            if ((ftValue < initUsdt || usdt < initUsdt) && tradeCount % initInterval == 0) {
                //需要去初始化了
                try {
                    if (isHaveInitBuyAndSell(ft, usdt, marketPrice, initUsdt, symbol, "limit")) {
                        //进行了两个币种的均衡，去进行余额查询，并判断是否成交完
                        logger.info("================有进行初始化均衡操作=================");
                        tradeCount++;
                        continue;
                    }
                } catch (Exception e) {//初始化失败，需要重新判断余额初始化
                    tradeCount = 0;
                    continue;
                }
            }

            //买单 卖单
            double price = Math.min(Math.min(ftBalance.getAvailable() * marketPrice, usdtBalance.getAvailable()), maxNum * marketPrice);

            BigDecimal ftAmount = getNum(price / marketPrice);
            tradeCount++;
            logger.info("=============================交易对开始=========================");
            try {
                buyNotLimit(symbol, "limit", ftAmount, getMarketPrice(marketPrice));
            } catch (Exception e) {
                logger.error("交易对买出错", e);
                tradeCount = 0;
            }

            try {
                sellNotLimit(symbol, "limit", ftAmount, getMarketPrice(marketPrice));
            } catch (Exception e) {
                logger.error("交易对卖出错", e);
                tradeCount = 0;
            }

            logger.info("=============================交易对结束=========================");
            Thread.sleep(1000);
        }
    }

    /**
     * 自买自卖交易
     *
     * @param symbol    交易对
     * @param ftName    交易币名称
     * @param usdtName  市场币名称
     * @param increment 收益率一半
     * @throws Exception
     */
    public void ftusdt1(String symbol, String ftName, String usdtName, double increment) throws Exception {

        while (true) {

            //查询余额
            String balance = null;
            try {
                balance = retryTemplate.execute(retryContext ->
                        getBalance()
                );
            } catch (Exception e) {
                logger.error("==========fcoinUtils.getBalance重试后还是异常============", e);
                continue;
            }

            Map<String, Balance> balances = buildBalance(balance);
            Balance ftBalance = balances.get(ftName);
            Balance usdtBalance = balances.get(usdtName);

            double ft = ftBalance.getBalance();
            double usdt = usdtBalance.getBalance();
            //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
            if (ftBalance.getFrozen() > 0.099 * ft || usdtBalance.getFrozen() > 0.099 * usdt) {
                Thread.sleep(3000);
                continue;
            }

            logger.info("===============balance: usdt:{},ft:{}========================", usdt, ft);
            Map<String, Double> priceInfo = getPriceInfo(symbol);
            Double marketPrice = priceInfo.get("marketPrice");
            //usdt小于51并且ft的价值小于51
            if ((usdt < (minUsdt + 1) && ft < ((minUsdt + 1) / marketPrice))
                    || (usdt < (minUsdt + 1) && Math.abs(ft * marketPrice - usdt) < 11)
                    || (ft < ((minUsdt + 1) / marketPrice) && Math.abs(ft * marketPrice - usdt) < 11)) {
                logger.info("跳出循环，ustd:{}, marketPrice:{}", usdt, marketPrice);
                break;
            }

            //ft:usdt=1:0.6
            double initUsdt = maxNum * initMultiple * marketPrice;

            //初始化
            if (isHaveInitBuyAndSell(ft, usdt, marketPrice, initUsdt, symbol, "limit")) {
                logger.info("================有进行初始化均衡操作=================");
                continue;
            }

            //买单 卖单
            double price = Math.min(ftBalance.getAvailable() * marketPrice, usdtBalance.getAvailable());

            BigDecimal ftAmount = getNum(price / marketPrice);

            logger.info("=============================交易对开始=========================");

            try {
                buyNotLimit(symbol, "limit", ftAmount, getMarketPrice(marketPrice * (1 - increment)));
            } catch (Exception e) {
                logger.error("交易对买出错", e);
            }
            try {
                sellNotLimit(symbol, "limit", ftAmount, getMarketPrice(marketPrice * (1 + increment)));
            } catch (Exception e) {
                logger.error("交易对卖出错", e);
            }
            logger.info("=============================交易对结束=========================");

            Thread.sleep(1000);
        }
    }

    /**
     * 做波段调用此函数
     *
     * @param symbol    交易对
     * @param ftName    交易币的名称
     * @param usdtName  市场币名称
     * @param increment 收益率的一半
     * @throws Exception
     */
    public void ftusdt2(String symbol, String ftName, String usdtName, double increment) throws Exception {

        while (true) {

            //查询余额
            String balance = null;
            try {
                balance = retryTemplate.execute(retryContext ->
                        getBalance()
                );
            } catch (Exception e) {
                logger.error("==========fcoinUtils.getBalance重试后还是异常============", e);
                continue;
            }

            Map<String, Balance> balances = buildBalance(balance);
            Balance ftBalance = balances.get(ftName);
            Balance usdtBalance = balances.get(usdtName);

            double ft = ftBalance.getBalance();
            double usdt = usdtBalance.getBalance();
            //判断是否有冻结的，如果冻结太多冻结就休眠，进行下次挖矿
            if (ftBalance.getFrozen() > 0.099 * ft || usdtBalance.getFrozen() > 0.099 * usdt) {
                Thread.sleep(3000);
                continue;
            }

            logger.info("===============balance: usdt:{},ft:{}========================", usdt, ft);
            Map<String, Double> priceInfo = getPriceInfo(symbol);
            Double marketPrice = priceInfo.get("marketPrice");
            //usdt小于51并且ft的价值小于51
            if ((usdt < (minUsdt + 1) && ft < ((minUsdt + 1) / marketPrice))
                    || (usdt < (minUsdt + 1) && Math.abs(ft * marketPrice - usdt) < 11)
                    || (ft < ((minUsdt + 1) / marketPrice) && Math.abs(ft * marketPrice - usdt) < 11)) {
                logger.info("跳出循环，ustd:{}, marketPrice:{}", usdt, marketPrice);
                break;
            }

            //在波段内才能交易
            double avgPrice = priceInfo.get("24HPrice");
            if (Math.abs(marketPrice - avgPrice) > avgPrice * 0.01) {
                Thread.sleep(3000);
                continue;
            }
            //ft:usdt=1:0.6
            double initUsdt = maxNum * initMultiple * marketPrice;

            //初始化
            if (isHaveInitBuyAndSell(ft, usdt, marketPrice, initUsdt, symbol, "limit")) {
                logger.info("================有进行初始化均衡操作=================");
                continue;
            }

            //买单 卖单
            double price = Math.min(ftBalance.getAvailable() * marketPrice, usdtBalance.getAvailable());

            BigDecimal ftAmount = getNum(price / marketPrice);

            logger.info("=============================交易对开始=========================");

            try {
                buyNotLimit(symbol, "limit", ftAmount, getMarketPrice(marketPrice * (1 - increment)));
            } catch (Exception e) {
                logger.error("交易对买出错", e);
            }
            try {
                sellNotLimit(symbol, "limit", ftAmount, getMarketPrice(marketPrice * (1 + increment)));
            } catch (Exception e) {
                logger.error("交易对卖出错", e);
            }
            logger.info("=============================交易对结束=========================");

            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) throws Exception {
        logger.info(getSymbols());
    }
}
