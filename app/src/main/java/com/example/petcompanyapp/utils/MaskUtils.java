package com.example.petcompanyapp.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public final class MaskUtils {

    private MaskUtils() {
        // Classe utilitaria para mascaras de texto.
    }

    public static void applyCnpjMask(EditText editText) {
        attachMask(editText, new TextWatcher() {
            private boolean isUpdating;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isUpdating) {
                    return;
                }

                String digits = editable.toString().replaceAll("\\D", "");
                StringBuilder builder = new StringBuilder();
                int length = Math.min(digits.length(), 14);

                for (int i = 0; i < length; i++) {
                    if (i == 2 || i == 5) {
                        builder.append('.');
                    } else if (i == 8) {
                        builder.append('/');
                    } else if (i == 12) {
                        builder.append('-');
                    }
                    builder.append(digits.charAt(i));
                }

                isUpdating = true;
                editText.setText(builder.toString());
                editText.setSelection(builder.length());
                isUpdating = false;
            }
        });
    }

    public static void applyCpfOrCnpjMask(EditText editText, boolean companyMode) {
        if (companyMode) {
            applyCnpjMask(editText);
            return;
        }

        attachMask(editText, new TextWatcher() {
            private boolean isUpdating;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isUpdating) {
                    return;
                }

                String digits = editable.toString().replaceAll("\\D", "");
                StringBuilder builder = new StringBuilder();
                int length = Math.min(digits.length(), 11);

                for (int i = 0; i < length; i++) {
                    if (i == 3 || i == 6) {
                        builder.append('.');
                    } else if (i == 9) {
                        builder.append('-');
                    }
                    builder.append(digits.charAt(i));
                }

                isUpdating = true;
                editText.setText(builder.toString());
                editText.setSelection(builder.length());
                isUpdating = false;
            }
        });
    }

    public static void applyPhoneMask(EditText editText) {
        attachMask(editText, new TextWatcher() {
            private boolean isUpdating;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isUpdating) {
                    return;
                }

                String digits = editable.toString().replaceAll("\\D", "");
                StringBuilder builder = new StringBuilder();
                int length = Math.min(digits.length(), 11);

                for (int i = 0; i < length; i++) {
                    if (i == 0) {
                        builder.append('(');
                    } else if (i == 2) {
                        builder.append(") ");
                    } else if (i == 7 && length > 10) {
                        builder.append('-');
                    } else if (i == 6 && length <= 10) {
                        builder.append('-');
                    }
                    builder.append(digits.charAt(i));
                }

                isUpdating = true;
                editText.setText(builder.toString());
                editText.setSelection(builder.length());
                isUpdating = false;
            }
        });
    }

    private static void attachMask(EditText editText, TextWatcher newWatcher) {
        Object currentTag = editText.getTag();
        if (currentTag instanceof TextWatcher) {
            editText.removeTextChangedListener((TextWatcher) currentTag);
        }
        editText.addTextChangedListener(newWatcher);
        editText.setTag(newWatcher);
    }
}
