# Contributing Guide

## Pull Request Process

### 1. Before Creating PR
- Create feature branch from `main`
- Follow naming convention: `feature/description` or `fix/description`
- Make atomic commits with clear messages

### 2. Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Examples:**
```bash
feat(merchant): add getAllMerchants endpoint

- Added pagination support with page and size parameters
- Implemented search functionality across merchant name and email
- Added sorting by multiple fields
- Updated MerchantController.java with new endpoint
- Updated MerchantService.java with business logic

Closes #123
```

```bash
fix(auth): resolve JWT token validation issue

- Fixed token expiration check in JwtUtil.java
- Updated SecurityConfig.java to handle expired tokens
- Added proper error response for invalid tokens

Fixes #456
```

### 3. PR Requirements
- Fill out PR template completely
- Link related issues using `Closes #123` or `Fixes #456`
- Ensure all tests pass
- Add/update documentation if needed
- Request review from team members

### 4. Code Standards
- Follow existing code style
- Add meaningful comments for complex logic
- Update API documentation for endpoint changes
- Include error handling
- Validate input parameters

### 5. File Change Documentation
When modifying files, document in PR:
- **Controllers**: New endpoints, parameter changes, response format updates
- **Services**: Business logic changes, new methods, algorithm updates
- **DTOs**: Field additions, validation changes, serialization updates
- **Config**: Security rules, database settings, application properties
- **Tests**: New test cases, test data updates, mock changes

### 6. API Changes Checklist
- [ ] Swagger/OpenAPI documentation updated
- [ ] Postman collection updated
- [ ] Request/Response examples provided
- [ ] Error codes documented
- [ ] Authentication requirements specified

## Quick Setup
```bash
# Set commit template
git config commit.template .gitmessage

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit  # Opens template

# Push and create PR
git push origin feature/your-feature-name
```