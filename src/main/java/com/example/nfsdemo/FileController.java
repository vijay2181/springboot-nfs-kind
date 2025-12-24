package com.example.nfsdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@RestController
@RequestMapping("/files")
public class FileController {

	    private static final Path ROOT = Paths.get("/data");

	        @PostMapping
		    public String writeFile(
				                @RequestParam(defaultValue = "test.txt") String name,
						            @RequestParam(defaultValue = "hello from springboot+nfs") String content
							        ) throws IOException {

			            if (!Files.exists(ROOT)) {
					                Files.createDirectories(ROOT);
							        }

				            Path target = ROOT.resolve(name);

					            Files.writeString(
								                    target,
										                    content + System.lineSeparator(),
												                    StandardOpenOption.CREATE,
														                    StandardOpenOption.APPEND
																            );

						            return "Written to " + target;
							        }

		    @GetMapping("/{name}")
		        public String readFile(@PathVariable String name) throws IOException {
				        Path target = ROOT.resolve(name);
					        if (!Files.exists(target)) {
							            return "File not found: " + target;
								            }
						        return Files.readString(target);
							    }
}
