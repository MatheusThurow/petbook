package com.petbook.app.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.adapters.PostCommentAdapter;
import com.petbook.app.models.AnimalPost;
import com.petbook.app.models.PostComment;
import com.petbook.app.repositories.AnimalPostRepository;
import com.petbook.app.repositories.FirebasePostRepository;
import com.petbook.app.repositories.NotificationRepository;
import com.petbook.app.repositories.PostCommentRepository;
import com.petbook.app.utils.BackNavigationUtils;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.NotificationType;
import com.petbook.app.utils.SessionUtils;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.ValidationUtils;

import java.util.List;

public class PostCommentsActivity extends AppCompatActivity {

    private Long currentUserId;
    private String currentUserName;
    private AnimalPost post;
    private PostCommentAdapter adapter;
    private TextView textEmptyComments;
    private EditText editComment;
    private TextView textReplyingTo;
    private PostComment replyingToComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionUtils.requireAuthenticated(this)) {
            return;
        }
        setContentView(R.layout.activity_post_comments);

        currentUserId = UserProfileStorage.getUserId(this);
        currentUserName = UserProfileStorage.getName(this, "");

        android.view.View buttonBack = findViewById(R.id.buttonBackComments);
        RecyclerView recyclerComments = findViewById(R.id.recyclerComments);
        textEmptyComments = findViewById(R.id.textEmptyComments);
        editComment = findViewById(R.id.editCommentMessage);
        TextView textSend = findViewById(R.id.textSendComment);

        BackNavigationUtils.bind(this, buttonBack);
        adapter = new PostCommentAdapter(this::startReplyToComment);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setAdapter(adapter);
        textReplyingTo = findViewById(R.id.textReplyingTo);

        textSend.setOnClickListener(v -> submitComment());
        textReplyingTo.setOnClickListener(v -> clearReplyTarget());

        long postId = getIntent().getLongExtra(IntentKeys.EXTRA_POST_ID, -1L);
        if (postId < 0) {
            BackNavigationUtils.navigateBack(this);
            return;
        }

        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.getPostById(this, postId, new FirebasePostRepository.PostCallback() {
                @Override
                public void onSuccess(AnimalPost loadedPost) {
                    post = loadedPost;
                    bindPostHeader();
                    loadComments();
                }

                @Override
                public void onError(String message) {
                    BackNavigationUtils.navigateBack(PostCommentsActivity.this);
                }
            });
        } else {
            post = AnimalPostRepository.findById(this, postId);
            if (post == null) {
                BackNavigationUtils.navigateBack(this);
                return;
            }
            bindPostHeader();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComments();
    }

    private void loadComments() {
        if (post == null) {
            return;
        }

        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.getComments(this, post.getId(), new FirebasePostRepository.CommentsCallback() {
                @Override
                public void onSuccess(List<PostComment> comments) {
                    adapter.submitList(comments);
                    textEmptyComments.setVisibility(comments.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                }

                @Override
                public void onError(String message) {
                    textEmptyComments.setVisibility(android.view.View.VISIBLE);
                }
            });
            return;
        }

        java.util.List<PostComment> comments = PostCommentRepository.getCommentsForPost(this, post.getId());
        adapter.submitList(comments);
        textEmptyComments.setVisibility(comments.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void submitComment() {
        if (currentUserId == null || post == null) {
            return;
        }

        String message = editComment.getText().toString().trim();
        if (ValidationUtils.isEmpty(message)) {
            editComment.setError(getString(R.string.error_comment_required));
            return;
        }

        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.addComment(
                    this,
                    post,
                    currentUserId,
                    currentUserName,
                    UserProfileStorage.getEmail(this, ""),
                    message,
                    replyingToComment == null ? null : replyingToComment.getId(),
                    new FirebasePostRepository.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            editComment.setText("");
                            clearReplyTarget();
                            loadComments();
                            Toast.makeText(PostCommentsActivity.this, R.string.comment_success, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(PostCommentsActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
            return;
        }

        PostCommentRepository.addComment(
                this,
                post.getId(),
                currentUserId,
                message,
                replyingToComment == null ? null : replyingToComment.getId()
        );
        editComment.setText("");
        clearReplyTarget();
        loadComments();

        if (post.getAuthorUserId() != null && post.getAuthorUserId() != currentUserId) {
            NotificationRepository.addNotification(
                    this,
                    post.getAuthorUserId(),
                    NotificationType.COMMENT,
                    getString(R.string.notification_comment_title),
                    getString(R.string.notification_comment_message, currentUserName, post.getAnimalName()),
                    post.getId(),
                    post.getPostType(),
                    currentUserId,
                    currentUserName,
                    UserProfileStorage.getEmail(this, ""),
                    null
            );
        }

        Toast.makeText(this, R.string.comment_success, Toast.LENGTH_SHORT).show();
    }

    private void startReplyToComment(PostComment comment) {
        replyingToComment = comment;
        textReplyingTo.setText(getString(R.string.comments_replying_to, comment.getAuthorName()));
        textReplyingTo.setVisibility(android.view.View.VISIBLE);
        editComment.requestFocus();
    }

    private void clearReplyTarget() {
        replyingToComment = null;
        textReplyingTo.setVisibility(android.view.View.GONE);
    }

    private void bindPostHeader() {
        TextView textTitle = findViewById(R.id.textCommentsTitle);
        TextView textSubtitle = findViewById(R.id.textCommentsSubtitle);
        textTitle.setText(post.getAnimalName());
        textSubtitle.setText(post.getDescription());
    }
}
