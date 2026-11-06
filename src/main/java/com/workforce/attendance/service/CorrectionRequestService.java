
package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.CorrectionRequestDTO;

public interface CorrectionRequestService {

    CorrectionRequestDTO submitCorrection(CorrectionRequestDTO dto);

    List<CorrectionRequestDTO> getEmployeeRequests(Long employeeId);

    List<CorrectionRequestDTO> getPendingRequestsForManager(Long managerId);

    CorrectionRequestDTO approveCorrection(Long requestId, Long managerId);

    CorrectionRequestDTO rejectCorrection(Long requestId, Long managerId);
}
