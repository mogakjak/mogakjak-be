package com.mogakjak.mogakjak;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MogakjakApplicationTests {

	@Autowired
	private AmazonS3 amazonS3;

	@Test
	void setCors() {
		CORSRule rule = new CORSRule()
				.withId("MogakjakCORSRule")
				.withAllowedMethods(List.of(
						CORSRule.AllowedMethods.GET,
						CORSRule.AllowedMethods.PUT,
						CORSRule.AllowedMethods.POST,
						CORSRule.AllowedMethods.HEAD
				))
				.withAllowedOrigins(List.of("*")) // 프론트엔드 주소
				.withAllowedHeaders(List.of("*"))
				.withExposedHeaders(List.of("ETag"))
				.withMaxAgeSeconds(3000);

		BucketCrossOriginConfiguration configuration = new BucketCrossOriginConfiguration();
		configuration.setRules(List.of(rule));

		try {
			amazonS3.setBucketCrossOriginConfiguration("mogakjak", configuration);
			System.out.println("=================================");
			System.out.println("🎉 CORS 설정이 성공적으로 적용되었습니다!");
			System.out.println("=================================");
		} catch (Exception e) {
			System.err.println("❌ CORS 설정 실패: " + e.getMessage());
			throw e;
		}
	}
}