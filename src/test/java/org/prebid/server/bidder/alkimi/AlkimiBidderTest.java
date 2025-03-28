package org.prebid.server.bidder.alkimi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iab.openrtb.request.Audio;
import com.iab.openrtb.request.Banner;
import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.request.Format;
import com.iab.openrtb.request.Imp;
import com.iab.openrtb.request.Video;
import com.iab.openrtb.response.Bid;
import com.iab.openrtb.response.BidResponse;
import com.iab.openrtb.response.SeatBid;
import org.junit.jupiter.api.Test;
import org.prebid.server.VertxTest;
import org.prebid.server.bidder.model.BidderBid;
import org.prebid.server.bidder.model.BidderCall;
import org.prebid.server.bidder.model.BidderError;
import org.prebid.server.bidder.model.HttpRequest;
import org.prebid.server.bidder.model.HttpResponse;
import org.prebid.server.bidder.model.Result;
import org.prebid.server.proto.openrtb.ext.ExtPrebid;
import org.prebid.server.proto.openrtb.ext.request.alkimi.ExtImpAlkimi;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.prebid.server.proto.openrtb.ext.response.BidType.audio;
import static org.prebid.server.proto.openrtb.ext.response.BidType.banner;
import static org.prebid.server.proto.openrtb.ext.response.BidType.video;

public class AlkimiBidderTest extends VertxTest {

    private static final String ENDPOINT_URL = "https://exchange.alkimi-onboarding.com/server/bid";
    private static final String DIV_BANNER_ID = "div_banner_1";
    private static final String DIV_VIDEO_ID = "div_video_1";
    private static final String DIV_AUDIO_ID = "div_audio_1";
    private static final String PUB_TOKEN = "testPubToken";

    private final AlkimiBidder target = new AlkimiBidder(ENDPOINT_URL, jacksonMapper);

    @Test
    public void creationShouldFailOnInvalidEndpointUrl() {
        assertThatIllegalArgumentException().isThrownBy(() -> new AlkimiBidder("invalid_url", jacksonMapper));
    }

    @Test
    public void makeHttpRequestsShouldUseCorrectURL() {
        final BidRequest bidRequest = givenBidRequest(impBuilder -> impBuilder.banner(Banner.builder().build()));
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .extracting(HttpRequest::getUri)
                .containsExactly(ENDPOINT_URL);
    }

    @Test
    public void makeHttpRequestsShouldUpdateImps() {
        final BidRequest bidRequest = givenBidRequest();
        final Result<List<HttpRequest<BidRequest>>> result = target.makeHttpRequests(bidRequest);

        final BidRequest expectedBidRequest = BidRequest.builder()
                .imp(List.of(Imp.builder()
                                .id(DIV_BANNER_ID)
                                .bidfloor(BigDecimal.valueOf(0.2))
                                .banner(expectedBanner())
                                .instl(1)
                                .exp(30)
                                .ext(expectedBannerExt())
                                .build(),
                        Imp.builder()
                                .id(DIV_VIDEO_ID)
                                .bidfloor(BigDecimal.valueOf(0.3))
                                .video(expectedVideo())
                                .instl(1)
                                .exp(30)
                                .ext(expectedVideoExt())
                                .build(),
                        Imp.builder()
                                .id(DIV_AUDIO_ID)
                                .bidfloor(BigDecimal.valueOf(0.4))
                                .audio(expectedAudio())
                                .instl(1)
                                .exp(30)
                                .ext(expectedAudioExt())
                                .build())
                ).build();

        assertThat(result.getValue())
                .extracting(HttpRequest::getPayload)
                .containsExactly(expectedBidRequest);
    }

    private Banner expectedBanner() {
        return Banner.builder()
                .format(Collections.singletonList(Format.builder()
                        .w(300)
                        .h(250)
                        .build())
                ).build();
    }

    private ObjectNode expectedBannerExt() {
        return mapper.valueToTree(Map.of(
                "tid", "12345",
                "gpid", "300x250",
                "bidder", ExtImpAlkimi.builder()
                        .token(PUB_TOKEN)
                        .bidFloor(BigDecimal.valueOf(0.2))
                        .instl(1)
                        .exp(30)
                        .adUnitCode(DIV_BANNER_ID)
                        .build()));
    }

    private Video expectedVideo() {
        return Video.builder()
                .w(1024)
                .h(768)
                .mimes(List.of("video/mp4"))
                .protocols(List.of(1, 2, 3, 4, 5))
                .build();
    }

    private ObjectNode expectedVideoExt() {
        return mapper.valueToTree(ExtPrebid.of(
                null,
                ExtImpAlkimi.builder()
                        .token(PUB_TOKEN)
                        .bidFloor(BigDecimal.valueOf(0.3))
                        .instl(1)
                        .exp(30)
                        .adUnitCode(DIV_VIDEO_ID)
                        .build()));
    }

    private Audio expectedAudio() {
        return Audio.builder()
                .mimes(List.of("audio/mp4"))
                .build();
    }

    private ObjectNode expectedAudioExt() {
        return mapper.valueToTree(ExtPrebid.of(
                null,
                ExtImpAlkimi.builder()
                        .token(PUB_TOKEN)
                        .bidFloor(BigDecimal.valueOf(0.4))
                        .instl(1)
                        .exp(30)
                        .adUnitCode(DIV_AUDIO_ID)
                        .build()));
    }

    @Test
    public void makeBidsShouldReturnErrorIfResponseBodyCouldNotBeParsed() {
        final BidderCall<BidRequest> httpCall = givenHttpCall(null, "invalid");
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        assertThat(result.getErrors())
                .hasSize(1)
                .allMatch(error -> error.getType() == BidderError.Type.bad_server_response
                        && error.getMessage().startsWith("Failed to decode: Unrecognized token"));
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnEmptyListIfBidResponseIsNull() throws JsonProcessingException {
        final BidderCall<BidRequest> httpCall = givenHttpCall(null, mapper.writeValueAsString(null));
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnEmptyListIfBidResponseSeatBidIsNull() throws JsonProcessingException {
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                null,
                mapper.writeValueAsString(BidResponse.builder().build()));
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }

    @Test
    public void makeBidsShouldReturnBidsForBannerAndVideoImps() throws JsonProcessingException {
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                givenBidRequest(),
                mapper.writeValueAsString(givenBidResponse()));
        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue()).contains(BidderBid.of(givenBannerBid(identity()), banner, null));
        assertThat(result.getValue()).contains(BidderBid.of(givenVideoBid(identity()), video, null));
        assertThat(result.getValue()).contains(BidderBid.of(givenAudioBid(identity()), audio, null));
    }

    @Test
    public void makeBidsShouldReturnBidWithResolvedMacros() throws JsonProcessingException {
        final BidderCall<BidRequest> httpCall = givenHttpCall(
                givenBidRequest(),
                mapper.writeValueAsString(givenBidResponse(
                        bidBuilder -> bidBuilder
                                .nurl("nurl:${AUCTION_PRICE}")
                                .adm("adm:${AUCTION_PRICE}")
                                .price(BigDecimal.TEN))));

        final Result<List<BidderBid>> result = target.makeBids(httpCall, null);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValue())
                .extracting(BidderBid::getBid)
                .extracting(Bid::getNurl, Bid::getAdm)
                .containsExactly(
                        tuple("nurl:10", "adm:10"),
                        tuple("nurl:10", "adm:10"),
                        tuple("nurl:10", "adm:10"));
    }

    private static BidRequest givenBidRequest() {
        return givenBidRequest(identity());
    }

    private static BidRequest givenBidRequest(Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {
        return givenBidRequest(identity(), impCustomizer);
    }

    private static BidRequest givenBidRequest(
            Function<BidRequest.BidRequestBuilder, BidRequest.BidRequestBuilder> bidRequestCustomizer,
            Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer
    ) {
        return bidRequestCustomizer.apply(BidRequest.builder()
                .imp(List.of(givenBannerImp(impCustomizer), givenVideoImp(impCustomizer), givenAudioImp(impCustomizer)))
        ).build();
    }

    private static Imp givenBannerImp(Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {
        return impCustomizer.apply(Imp.builder()
                .id(DIV_BANNER_ID)
                .banner(Banner.builder()
                        .format(Collections.singletonList(Format.builder()
                                .w(300)
                                .h(250)
                                .build())
                        ).build())
                .ext(mapper.valueToTree(Map.of(
                        "tid", "12345",
                        "gpid", "300x250",
                        "bidder", ExtImpAlkimi.builder()
                                .token(PUB_TOKEN)
                                .bidFloor(BigDecimal.valueOf(0.2))
                                .instl(1)
                                .exp(30)
                                .build())))
        ).build();
    }

    private static Imp givenVideoImp(Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {
        return impCustomizer.apply(Imp.builder()
                .id(DIV_VIDEO_ID)
                .video(Video.builder()
                        .w(1024)
                        .h(768)
                        .mimes(List.of("video/mp4"))
                        .protocols(List.of(1, 2, 3, 4, 5))
                        .build())
                .ext(mapper.valueToTree(ExtPrebid.of(
                        null,
                        ExtImpAlkimi.builder()
                                .token(PUB_TOKEN)
                                .bidFloor(BigDecimal.valueOf(0.3))
                                .instl(1)
                                .exp(30)
                                .build())))
        ).build();
    }

    private static Imp givenAudioImp(Function<Imp.ImpBuilder, Imp.ImpBuilder> impCustomizer) {
        return impCustomizer.apply(Imp.builder()
                .id(DIV_AUDIO_ID)
                .audio(Audio.builder()
                        .mimes(List.of("audio/mp4"))
                        .build())
                .ext(mapper.valueToTree(ExtPrebid.of(
                        null,
                        ExtImpAlkimi.builder()
                                .token(PUB_TOKEN)
                                .bidFloor(BigDecimal.valueOf(0.4))
                                .instl(1)
                                .exp(30)
                                .build())))
        ).build();
    }

    private static BidResponse givenBidResponse() {
        return givenBidResponse(identity());
    }

    private static BidResponse givenBidResponse(Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer) {
        return givenBidResponse(identity(), bidCustomizer);
    }

    private static BidResponse givenBidResponse(
            Function<BidResponse.BidResponseBuilder, BidResponse.BidResponseBuilder> bidResponseCustomizer,
            Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer
    ) {
        return bidResponseCustomizer.apply(BidResponse.builder()
                .seatbid(singletonList(SeatBid.builder().bid(List.of(
                        givenBannerBid(bidCustomizer),
                        givenVideoBid(bidCustomizer),
                        givenAudioBid(bidCustomizer))
                ).build()))
        ).build();
    }

    private static Bid givenBannerBid(Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer) {
        return bidCustomizer.apply(Bid.builder()
                .impid(DIV_BANNER_ID)
        ).build();
    }

    private static Bid givenVideoBid(Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer) {
        return bidCustomizer.apply(Bid.builder()
                .impid(DIV_VIDEO_ID)
        ).build();
    }

    private static Bid givenAudioBid(Function<Bid.BidBuilder, Bid.BidBuilder> bidCustomizer) {
        return bidCustomizer.apply(Bid.builder()
                .impid(DIV_AUDIO_ID)
        ).build();
    }

    private static BidderCall<BidRequest> givenHttpCall(BidRequest bidRequest, String body) {
        return BidderCall.succeededHttp(
                HttpRequest.<BidRequest>builder().payload(bidRequest).build(),
                HttpResponse.of(200, null, body),
                null);
    }
}
