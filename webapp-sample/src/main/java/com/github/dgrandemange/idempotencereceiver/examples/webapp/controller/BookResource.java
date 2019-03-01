package com.github.dgrandemange.idempotencereceiver.examples.webapp.controller;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.dgrandemange.idempotencereceiver.api.annot.Idempotent;
import com.github.dgrandemange.idempotencereceiver.examples.webapp.exception.ResourceAlreadyExistsException;
import com.github.dgrandemange.idempotencereceiver.examples.webapp.exception.ResourceManagementException;
import com.github.dgrandemange.idempotencereceiver.examples.webapp.exception.ResourceNotFoundException;
import com.github.dgrandemange.idempotencereceiver.examples.webapp.model.dto.Book;
import com.github.dgrandemange.idempotencereceiver.examples.webapp.model.dto.ResourceIdentifier;

@RestController
@RequestMapping("/books")
public class BookResource {

	AtomicLong bookSeq = new AtomicLong();

	/**
	 * in-memory book storage facility<br>
	 */
	Map<Long, Book> booksById = new ConcurrentHashMap<>();

	@PostMapping
	@Idempotent(registerableEx = { ResourceManagementException.class })
	ResponseEntity<ResourceIdentifier> createWithBodyReturned(@RequestBody Book bookDTO, UriComponentsBuilder ucb) {
		Long id = findByBook(bookDTO);

		if (Objects.isNull(id)) {
			id = bookSeq.incrementAndGet();
			booksById.put(id, bookDTO);

			UriComponents uriComponents = ucb.path("/books/{id}").buildAndExpand(id);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setLocation(uriComponents.toUri());

			ResourceIdentifier resId = new ResourceIdentifier();
			resId.setId(Long.toString(id));

			return ResponseEntity.created(uriComponents.toUri()).body(resId);
		} else {
			throw new ResourceAlreadyExistsException(Long.toString(id));
		}
	}

	ResponseEntity<?> createWithoutBodyReturned(@RequestBody Book bookDTO, UriComponentsBuilder ucb) {
		Long id = findByBook(bookDTO);

		if (Objects.isNull(id)) {
			id = bookSeq.incrementAndGet();
			booksById.put(id, bookDTO);

			UriComponents uriComponents = ucb.path("/books/{id}").buildAndExpand(id);
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setLocation(uriComponents.toUri());

			return ResponseEntity.created(uriComponents.toUri()).build();
		} else {
			throw new ResourceAlreadyExistsException(Long.toString(id));
		}
	}
	
	@GetMapping("/{id}")
	ResponseEntity<Book> read(@PathVariable Long id) {
		Book book = findById(id);

		if (Objects.isNull(book)) {
			throw new ResourceNotFoundException(Long.toString(id));
		} else {
			return ResponseEntity.ok(book);
		}
	}

	@DeleteMapping("/{id}")
	ResponseEntity<String> remove(@PathVariable Long id) {
		if (booksById.containsKey(id)) {
			booksById.remove(id);
			return ResponseEntity.noContent().build();
		} else {
			throw new ResourceNotFoundException(Long.toString(id));
		}
	}

	Book findById(Long id) {
		return booksById.get(id);
	}

	Long findByBook(Book bookDTO) {
		Long id = null;
		for (Entry<Long, Book> e : booksById.entrySet()) {
			if (e.getValue().equals(bookDTO)) {
				id = e.getKey();
				break;
			}
		}
		return id;
	}

}
