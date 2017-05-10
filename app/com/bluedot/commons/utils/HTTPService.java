package com.bluedot.commons.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HTTPService
{
	
	final static Logger logger = LoggerFactory.getLogger(HTTPService.class);
	
	public static ResponseWrapper get(String url, Map<String, String> headers) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		return get(url, headers, null, null);
	}
	
	public static ResponseWrapper get(String url, Map<String, String> headers, int timeout) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		return get(url, headers, null, null, timeout);
	}
	
	public static ResponseWrapper get(String url, Map<String, String> headers, String username, String password) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException,
	IOException
	{
		return get(url, headers, null, null, -1);
	}

	public static ResponseWrapper get(String url, Map<String, String> headers, String username, String password, int timeout) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException,
			IOException
	{
		RequestConfig conf = null;
		
		if(timeout > -1)
			conf = RequestConfig.custom().setSocketTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setConnectTimeout(timeout * 1000).build();
		else
			conf = RequestConfig.custom().setSocketTimeout(60 * 1000).setConnectionRequestTimeout(90 * 1000).setConnectTimeout(90 * 1000).build();
		
		HttpGet get = new HttpGet();
		return executeRequest(url, get, headers, username, password, conf);
	}

	public static ResponseWrapper getRaw(URI uri, Map<String, String> headers) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		// Create the HTTP Client
		CloseableHttpClient client = getHTTPClient();
		HttpGet get = new HttpGet(uri);

		if (headers != null)
		{
			for (String key : headers.keySet())
			{
				get.addHeader(key, headers.get(key));
			}
		}
		// Execute the GET
		HttpResponse response = client.execute(get);
		try{
			return new ResponseWrapper(EntityUtils.toByteArray(response.getEntity()), response.getStatusLine().getStatusCode());
		}finally{
			EntityUtils.consume(response.getEntity());
			client.close();
		}
	}

	public static ResponseWrapper delete(String url, Map<String, String> headers) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		// Create the HTTP Client
		HttpDelete delete = new HttpDelete();
		return executeRequest(url, delete, headers);
	}

	public static ResponseWrapper post(String url, Map<String, String> headers, byte[] data) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		return post(url, headers, data, null, null);
	}

	public static ResponseWrapper post(String url, Map<String, String> headers, byte[] data, String username, String password) throws KeyManagementException, NoSuchAlgorithmException,
			ClientProtocolException, IOException
	{
		HttpPost post = new HttpPost(url);

		if (data != null)
			post.setEntity(new ByteArrayEntity(data));

		return executeRequest(url, post, headers, username, password);
	}

	public static ResponseWrapper put(String url, Map<String, String> headers, byte[] data) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		HttpPut put = new HttpPut();
		if (data != null)
			put.setEntity(new ByteArrayEntity(data));
		return executeRequest(url, put, headers);
	}

	public static ResponseWrapper executeRequest(String urlString, HttpRequestBase request, Map<String, String> headers) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		return executeRequest(urlString, request, headers, null, null, null);
	}

	public static ResponseWrapper executeRequest(String urlString, HttpRequestBase request, Map<String, String> headers, RequestConfig conf) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		return executeRequest(urlString, request, headers, null, null, conf);
	}
	
	public static ResponseWrapper executeRequest(String urlString, HttpRequestBase request, Map<String, String> headers, String username, String password) throws KeyManagementException, NoSuchAlgorithmException,
	ClientProtocolException, IOException
	{
		return executeRequest(urlString, request, headers, username, password, null);
	}
	
	public static ResponseWrapper executeRequest(String urlString, HttpRequestBase request, Map<String, String> headers, String username, String password, RequestConfig conf) throws KeyManagementException, NoSuchAlgorithmException,
			ClientProtocolException, IOException
	{
		URL url = new URL(urlString);
		URI uri = null;
		try
		{
			uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		} catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		
		request.setURI(uri);
		
		CloseableHttpClient client = getHTTPClient(conf, username, password);

		if (headers != null)
		{
			for (String key : headers.keySet())
			{
				request.addHeader(key, headers.get(key));
			}
		}
		
		// Execute the GET
		HttpResponse response = client.execute(request);
		
		InputStream is = null;
		BufferedReader reader = null;
		try
		{
		
			// Get the response from location
			if (response.getEntity() != null)
			{
			
				is = response.getEntity().getContent();
				reader = new BufferedReader(new InputStreamReader(is));
				
				StringBuilder builder = new StringBuilder();
				String aux = "";

				while ((aux = reader.readLine()) != null)
				{
					builder.append(aux);
				}

				String text = builder.toString();

				logger.debug("RECEIVED: " + text + " |===");

				return new ResponseWrapper(text, response.getStatusLine().getStatusCode());
			
			} else
				return new ResponseWrapper("", response.getStatusLine().getStatusCode());
		} finally
		{
			if(is != null)
				is.close();
			if(reader != null)
				reader.close();
			if(response.getEntity() != null)
				EntityUtils.consume(response.getEntity());
			
			client.close();
		}
	}

	public static CloseableHttpClient getHTTPClient() throws KeyManagementException, NoSuchAlgorithmException
	{
		return getHTTPClient(null, null, null);
	}
	
	public static CloseableHttpClient getHTTPClient(RequestConfig conf) throws KeyManagementException, NoSuchAlgorithmException
	{
		return getHTTPClient(conf, null, null);
	}

	public static CloseableHttpClient getHTTPClient(RequestConfig conf, String username, String password) throws KeyManagementException, NoSuchAlgorithmException
	{
		SSLContext sslContext = SSLContext.getInstance("TLS");

		// set up a TrustManager that trusts everything
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers()
			{
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType)
			{
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType)
			{
			}
		} }, new SecureRandom());

		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslContext)).build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(cm).setSSLSocketFactory(csf);

		if (username != null && password != null)
		{
			CredentialsProvider provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
			provider.setCredentials(AuthScope.ANY, credentials);
			httpClientBuilder.setDefaultCredentialsProvider(provider);
		}
		
		if(conf != null)
		{
			httpClientBuilder.setDefaultRequestConfig(conf);
		}

		return httpClientBuilder.build();
	}
	
	public static class ResponseWrapper
	{
		private String body;
		private byte[] rawBody;
		private int resultCode;

		public ResponseWrapper(String body, int resultCode) {
			super();
			this.body = body;
			this.resultCode = resultCode;
		}

		public ResponseWrapper(byte[] body, int resultCode) {
			super();
			this.rawBody = body;
			this.resultCode = resultCode;
		}

		public String getBody()
		{
			return body;
		}

		public void setBody(String body)
		{
			this.body = body;
		}

		public int getResultCode()
		{
			return resultCode;
		}

		public void setResultCode(int resultCode)
		{
			this.resultCode = resultCode;
		}

		public byte[] getRawBody()
		{
			return rawBody;
		}

		public void setRawBody(byte[] rawBody)
		{
			this.rawBody = rawBody;
		}

		@Override
		public String toString()
		{
			return getResultCode() + " - " + getBody();
		}
	}
}
