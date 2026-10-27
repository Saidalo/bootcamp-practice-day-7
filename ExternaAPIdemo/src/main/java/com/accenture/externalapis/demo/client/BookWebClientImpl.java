package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return this.webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookApiResponse.class)
                .map(this::toBookDTO)
                .onErrorMap(this::isNotClientException, this::toClientException);
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return this.webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .map(this::toBookDTO)
                .onErrorMap(this::isNotClientException, this::toClientException);
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        return Mono.zip(getBookAsync(id1), getBookAsync(id2)).map(tuple -> List.of(tuple.getT1(), tuple.getT2()));
    }

    private BookDto toBookDTO(BookApiResponse response) {
        return new BookDto(response.title(), response.author(), response.genre(), response.price());
    }

    private boolean isNotClientException(Throwable e) {
        return !(e instanceof ClientException);
    }

    private ClientException toClientException(Throwable e) {
        if (e instanceof WebClientResponseException responseException) {
            return new ClientException("External service returned " + responseException.getStatusCode(), e);
        }
        if (e instanceof WebClientRequestException) {
            return new ClientException("External service is unreachable", e);
        }
        return new ClientException("Unexpected error while calling the external service", e);
    }
}
