package uk.gov.nationalarchives.tdr.api.service

import uk.gov.nationalarchives.tdr.api.db.repository.TransferAgreementRepository
import uk.gov.nationalarchives.tdr.api.graphql.fields.TransferAgreementFields.{AddTransferAgreementInput, TransferAgreement}
import uk.gov.nationalarchives.Tables.TransferagreementRow

import scala.concurrent.{ExecutionContext, Future}

class TransferAgreementService(transferAgreementRepository: TransferAgreementRepository)
                              (implicit val executionContext: ExecutionContext) {

  def addTransferAgreement(input: AddTransferAgreementInput): Future[TransferAgreement] = {
    val transferAgreementRow = TransferagreementRow(input.consignmentId,
      input.allPublicRecords,
      input.allCrownCopyright,
      input.allEnglish,
      input.allDigital,
      input.appraisalSelectionSignedOff,
      input.sensitivityReviewSignedOff)

    transferAgreementRepository.addTransferAgreement(transferAgreementRow).map(dbRowToTransferAgreement)
  }

  def getTransferAgreement(consignmentId: Long): Future[Option[TransferAgreement]] = {
    transferAgreementRepository.getTransferAgreement(consignmentId)
      .map(ta => ta.headOption.map(dbRowToTransferAgreement))
  }

  private def dbRowToTransferAgreement(row: TransferagreementRow ): TransferAgreement = {
    TransferAgreement(row.consignmentid,
      row.allpublicrecords,
      row.allcrowncopyright,
      row.allenglish,
      row.alldigital,
      row.appraisalselectionsignedoff,
      row.sensitivityreviewsignedoff,
      row.transferagreementid
    )
  }
}
