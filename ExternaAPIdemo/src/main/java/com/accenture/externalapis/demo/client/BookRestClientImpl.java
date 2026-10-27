package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.util.Arrays;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .build();

    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = this.restClient.get().uri("/books/{id}", id).retrieve().body(BookApiResponse.class);
            return toBookDTO(response);
        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            throw new ClientException(e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ClientException("Unexpected error:" + e.getMessage(), e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] books = this.restClient.get().uri("/books").retrieve().body(BookApiResponse[].class);
            if (books == null) {
                return List.of();
            }
            return Arrays.stream(books).map(this::toBookDTO).toList();
        } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
            throw new ClientException(e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ClientException("Unexpected error:" + e.getMessage(), e);
        }
    }

    public BookDto toBookDTO(BookApiResponse response) {
        if (response == null) {
            throw new ClientException("Received empty response from external client");
        }
        return new BookDto(response.title(), response.author(), response.genre(), response.price());
    }
}
